package com.example.mely.stocks.service;

import com.example.mely.stocks.model.DocumentExtractionStatus;
import com.example.mely.stocks.model.RuleProfile;
import com.example.mely.stocks.model.StockScreeningDocument;
import com.example.mely.stocks.model.StockScreeningRequest;
import com.example.mely.stocks.model.StockScreeningResult;
import com.example.mely.stocks.model.StockScreeningStatus;
import com.example.mely.stocks.repository.RuleProfileRepository;
import com.example.mely.stocks.repository.StockScreeningDocumentRepository;
import com.example.mely.stocks.repository.StockScreeningRequestRepository;
import com.example.mely.stocks.repository.StockScreeningResultRepository;
import com.example.mely.stocks.service.analysis.ActivityAnalysisResult;
import com.example.mely.stocks.service.analysis.ExtractedMetric;
import com.example.mely.stocks.service.analysis.ExtractionResult;
import com.example.mely.stocks.service.analysis.ResolvedMetric;
import com.example.mely.stocks.service.analysis.ScreeningComputation;
import com.example.mely.stocks.service.analysis.TatheerComputation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StockScreeningProcessingService {

    private final StockScreeningRequestRepository requestRepository;
    private final StockScreeningResultRepository resultRepository;
    private final StockScreeningDocumentRepository documentRepository;
    private final RuleProfileRepository ruleProfileRepository;
    private final FinancialMetricsExtractor financialMetricsExtractor;
    private final BusinessActivityAnalyzer businessActivityAnalyzer;
    private final ScreeningRulesEngine screeningRulesEngine;
    private final TatheerCalculator tatheerCalculator;
    private final ExplanationComposer explanationComposer;

    public StockScreeningProcessingService(StockScreeningRequestRepository requestRepository,
                                           StockScreeningResultRepository resultRepository,
                                           StockScreeningDocumentRepository documentRepository,
                                           RuleProfileRepository ruleProfileRepository,
                                           FinancialMetricsExtractor financialMetricsExtractor,
                                           BusinessActivityAnalyzer businessActivityAnalyzer,
                                           ScreeningRulesEngine screeningRulesEngine,
                                           TatheerCalculator tatheerCalculator,
                                           ExplanationComposer explanationComposer) {
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
        this.documentRepository = documentRepository;
        this.ruleProfileRepository = ruleProfileRepository;
        this.financialMetricsExtractor = financialMetricsExtractor;
        this.businessActivityAnalyzer = businessActivityAnalyzer;
        this.screeningRulesEngine = screeningRulesEngine;
        this.tatheerCalculator = tatheerCalculator;
        this.explanationComposer = explanationComposer;
    }

    @Transactional(noRollbackFor = Exception.class)
    public StockScreeningRequest processRequest(UUID requestId) {
        StockScreeningRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Stock screening request not found"));
        RuleProfile ruleProfile = ruleProfileRepository.findFirstByActiveTrueOrderByCreatedAtAsc()
                .orElseThrow(() -> new IllegalStateException("No active halal stock rule profile is configured"));

        try {
            request.setStatus(StockScreeningStatus.PROCESSING);
            List<StockScreeningDocument> documents = documentRepository.findByRequest_Id(requestId);
            if (!documents.isEmpty()) {
                documents.forEach(document -> document.setExtractionStatus(DocumentExtractionStatus.PROCESSING));
                documentRepository.saveAll(documents);
            }

            // Manual screening - use financial metrics from form input
            ExtractionResult extractionResult = financialMetricsExtractor.extract(request, documents);

            Map<String, ResolvedMetric> metrics = mergeMetrics(request.getManualMetrics(), extractionResult.getMetrics());
            ActivityAnalysisResult activityAnalysisResult = businessActivityAnalyzer.analyze(request, ruleProfile);
            ScreeningComputation computation = screeningRulesEngine.evaluate(ruleProfile, metrics, activityAnalysisResult);
            TatheerComputation tatheerComputation = tatheerCalculator.calculate(metrics, computation.isHalalEligible());

            List<String> warnings = new ArrayList<>(computation.getWarnings());
            if (extractionResult.getWarnings() != null) {
                warnings.addAll(extractionResult.getWarnings());
            }

            StockScreeningResult result = resultRepository.findByRequest_Id(requestId)
                    .orElse(StockScreeningResult.builder()
                            .organizationId(request.getOrganizationId())
                            .createdByUserId(request.getCreatedByUserId())
                            .request(request)
                            .build());

            result.setVerdict(computation.getVerdict());
            result.setNeedsTatheer(tatheerComputation.isNeedsTatheer());
            result.setTatheerAmount(tatheerComputation.getTatheerAmount());
            result.setPurificationRatio(tatheerComputation.getPurificationRatio());
            result.setPurificationPerShare(tatheerComputation.getPurificationPerShare());
            result.setMetricsUsed(computation.getMetricsUsed());
            result.setRuleResults(computation.getRuleResults());
            result.setWarnings(warnings);
            result.setUserMessage(explanationComposer.composeUserMessage(request, computation, activityAnalysisResult));
            result.setExplanation(explanationComposer.composeDetailedExplanation(request, computation, activityAnalysisResult));
            resultRepository.save(result);

            request.setResult(result);
            request.setWarnings(warnings);
            request.setStatus(StockScreeningStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
            request.setProcessingError(null);
            requestRepository.save(request);

            documents.forEach(document -> document.setExtractionStatus(DocumentExtractionStatus.COMPLETED));
            documentRepository.saveAll(documents);
            return request;
        } catch (Exception ex) {
            request.setStatus(StockScreeningStatus.FAILED);
            request.setProcessingError(ex.getMessage());
            request.setWarnings(List.of("Processing failed: " + ex.getMessage()));
            requestRepository.save(request);

            List<StockScreeningDocument> documents = documentRepository.findByRequest_Id(requestId);
            documents.forEach(document -> document.setExtractionStatus(DocumentExtractionStatus.FAILED));
            documentRepository.saveAll(documents);
            throw ex;
        }
    }

    private Map<String, ResolvedMetric> mergeMetrics(Map<String, Object> manualMetrics, Map<String, ExtractedMetric> extractedMetrics) {
        Map<String, ResolvedMetric> metrics = new LinkedHashMap<>();

        if (extractedMetrics != null) {
            extractedMetrics.forEach((key, extracted) -> metrics.put(key, ResolvedMetric.builder()
                    .value(extracted.getValue())
                    .source("extracted")
                    .sourceDocument(extracted.getSourceDocument())
                    .pageNumber(extracted.getPageNumber())
                    .snippet(extracted.getSnippet())
                    .build()));
        }

        if (manualMetrics != null) {
            manualMetrics.forEach((key, value) -> {
                if (value != null) {
                    metrics.put(key, ResolvedMetric.builder()
                            .value(new BigDecimal(value.toString()))
                            .source("manual")
                            .build());
                }
            });
        }

        return metrics;
    }
}
