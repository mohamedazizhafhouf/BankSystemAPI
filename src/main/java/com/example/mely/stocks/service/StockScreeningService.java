package com.example.mely.stocks.service;

import com.example.mely.security.AuthenticatedUser;
import com.example.mely.security.CurrentAuthenticatedUserProvider;
import com.example.mely.stocks.dto.RuleResultResponse;
import com.example.mely.stocks.dto.StockExplanationResponse;
import com.example.mely.stocks.dto.StockScreeningHistoryItemResponse;
import com.example.mely.stocks.dto.StockScreeningHistoryResponse;
import com.example.mely.stocks.dto.StockScreeningRequestForm;
import com.example.mely.stocks.dto.StockScreeningResponse;
import com.example.mely.stocks.model.AnalysisMode;
import com.example.mely.stocks.model.DocumentExtractionStatus;
import com.example.mely.stocks.model.StockScreeningDocument;
import com.example.mely.stocks.model.StockScreeningRequest;
import com.example.mely.stocks.model.StockScreeningResult;
import com.example.mely.stocks.model.StockScreeningStatus;
import com.example.mely.stocks.model.StockVerdict;
import com.example.mely.stocks.repository.StockScreeningDocumentRepository;
import com.example.mely.stocks.repository.StockScreeningRequestRepository;
import com.example.mely.stocks.repository.StockScreeningResultRepository;
import com.example.mely.stocks.service.impl.LocalDocumentStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class StockScreeningService {

    private static final List<String> REQUIRED_SYNC_METRICS = List.of(
            "interestBearingDebt",
            "cashAndInterestBearingAssets",
            "totalAssetsOrMarketCap",
            "totalRevenue",
            "nonCompliantIncome"
    );

    private final StockScreeningRequestRepository requestRepository;
    private final StockScreeningResultRepository resultRepository;
    private final StockScreeningDocumentRepository documentRepository;
    private final LocalDocumentStorageService documentStorageService;
    private final StockScreeningProcessingService processingService;
    private final StockScreeningAsyncProcessor asyncProcessor;
    private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;

    public StockScreeningService(StockScreeningRequestRepository requestRepository,
                                 StockScreeningResultRepository resultRepository,
                                 StockScreeningDocumentRepository documentRepository,
                                 LocalDocumentStorageService documentStorageService,
                                 StockScreeningProcessingService processingService,
                                 StockScreeningAsyncProcessor asyncProcessor,
                                 CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider) {
        this.requestRepository = requestRepository;
        this.resultRepository = resultRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.processingService = processingService;
        this.asyncProcessor = asyncProcessor;
        this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
    }

    public StockScreeningResponse submit(StockScreeningRequestForm form) {
        AuthenticatedUser actor = currentAuthenticatedUserProvider.getCurrentUser();
        UUID organizationId = actor.getOrganizationId();
        List<MultipartFile> documents = sanitizeDocuments(form.getDocuments());
        validateDocuments(documents);

        AnalysisMode requestedMode = resolveRequestedMode(form.getMode());
        boolean hasDocuments = !documents.isEmpty();
        boolean hasAllRequiredSyncMetrics = hasAllRequiredSyncMetrics(form);

        if (requestedMode == AnalysisMode.SYNC && (hasDocuments || !hasAllRequiredSyncMetrics)) {
            throw new IllegalArgumentException("SYNC mode requires manual financial inputs for all core ratios and does not support uploaded documents.");
        }

        AnalysisMode effectiveMode = hasDocuments || requestedMode == AnalysisMode.ASYNC
                ? AnalysisMode.ASYNC
                : AnalysisMode.SYNC;

        StockScreeningRequest request = StockScreeningRequest.builder()
                .organizationId(organizationId)
                .createdByUserId(actor.getUserId())
                .companyName(form.getCompanyName())
                .ticker(form.getTicker())
                .countryOrMarket(form.getCountryOrMarket())
                .currency(form.getCurrency())
                .businessDescription(form.getBusinessDescription())
                .sector(form.getSector())
                .fiscalYear(form.getFiscalYear())
                .reportingPeriod(form.getReportingPeriod())
                .requestedMode(effectiveMode)
                .status(StockScreeningStatus.SUBMITTED)
                .manualMetrics(buildManualMetrics(form))
                .warnings(new ArrayList<>())
                .build();
        request = requestRepository.saveAndFlush(request);

        List<StockScreeningDocument> persistedDocuments = new ArrayList<>();
        for (MultipartFile document : documents) {
            LocalDocumentStorageService.StoredDocument storedDocument = documentStorageService.store(request.getId(), document);
            StockScreeningDocument entity = StockScreeningDocument.builder()
                    .organizationId(organizationId)
                    .createdByUserId(actor.getUserId())
                    .request(request)
                    .originalFileName(document.getOriginalFilename() == null ? "document.pdf" : document.getOriginalFilename())
                    .storedFileName(storedDocument.getStoredFileName())
                    .storagePath(storedDocument.getStoragePath())
                    .contentType(storedDocument.getContentType())
                    .size(storedDocument.getSize())
                    .checksumSha256(storedDocument.getChecksum())
                    .extractionStatus(DocumentExtractionStatus.PENDING)
                    .sourceMetadata(Map.of("storagePath", storedDocument.getStoragePath()))
                    .build();
            persistedDocuments.add(entity);
        }
        if (!persistedDocuments.isEmpty()) {
            documentRepository.saveAll(persistedDocuments);
        }

        if (effectiveMode == AnalysisMode.SYNC) {
            StockScreeningRequest processedRequest = processingService.processRequest(request.getId());
            return toResponse(processedRequest, processedRequest.getResult());
        }

        request.setStatus(StockScreeningStatus.PROCESSING);
        requestRepository.save(request);
        asyncProcessor.process(request.getId());
        return toResponse(request, null);
    }

    public StockScreeningResponse getById(UUID requestId) {
        StockScreeningRequest request = resolveAccessibleRequest(requestId);
        StockScreeningResult result = resultRepository.findByRequest_Id(requestId).orElse(null);
        return toResponse(request, result);
    }

    public StockExplanationResponse getExplanation(UUID requestId) {
        StockScreeningRequest request = resolveAccessibleRequest(requestId);
        StockScreeningResult result = resultRepository.findByRequest_Id(requestId).orElse(null);

        return StockExplanationResponse.builder()
                .requestId(request.getId())
                .status(request.getStatus())
                .verdict(result != null ? result.getVerdict() : null)
                .explanation(result != null ? result.getExplanation() : null)
                .userMessage(result != null ? result.getUserMessage() : null)
                .warnings(result != null ? result.getWarnings() : request.getWarnings())
                .completedAt(request.getCompletedAt())
                .build();
    }

    public StockScreeningHistoryResponse getHistory(int page, int size, String rawStatus, String rawVerdict, String rawQuery) {
        validateHistoryPagination(page, size);

        AuthenticatedUser actor = currentAuthenticatedUserProvider.getCurrentUser();
        StockScreeningStatus status = parseStatus(rawStatus);
        StockVerdict verdict = parseVerdict(rawVerdict);
        String query = normalizeQuery(rawQuery);

        Page<StockScreeningRequest> historyPage = requestRepository.findHistoryByCreatedByUserId(
                actor.getUserId(),
                status,
                verdict,
                query,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return StockScreeningHistoryResponse.builder()
                .content(historyPage.getContent().stream().map(this::toHistoryItem).toList())
                .page(historyPage.getNumber())
                .size(historyPage.getSize())
                .totalElements(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .hasNext(historyPage.hasNext())
                .build();
    }

    private StockScreeningRequest resolveAccessibleRequest(UUID requestId) {
        AuthenticatedUser actor = currentAuthenticatedUserProvider.getCurrentUser();
        if (actor.hasOrganizationContext()) {
            return requestRepository.findByIdAndOrganizationId(requestId, actor.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Stock screening request not found"));
        }
        return requestRepository.findByIdAndCreatedByUserId(requestId, actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Stock screening request not found"));
    }

    private void validateHistoryPagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    private StockScreeningStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return StockScreeningStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status must be one of SUBMITTED, PROCESSING, COMPLETED, FAILED");
        }
    }

    private StockVerdict parseVerdict(String rawVerdict) {
        if (rawVerdict == null || rawVerdict.isBlank()) {
            return null;
        }
        try {
            return StockVerdict.valueOf(rawVerdict.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("verdict must be one of HALAL, NON_HALAL, REVIEW_REQUIRED");
        }
    }

    private String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        return rawQuery.trim().toLowerCase(Locale.ROOT);
    }

    private List<MultipartFile> sanitizeDocuments(List<MultipartFile> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                .filter(document -> document != null && !document.isEmpty())
                .toList();
    }

    private void validateDocuments(List<MultipartFile> documents) {
        for (MultipartFile document : documents) {
            String contentType = document.getContentType();
            String fileName = document.getOriginalFilename() == null ? "" : document.getOriginalFilename().toLowerCase();
            boolean pdf = "application/pdf".equalsIgnoreCase(contentType) || fileName.endsWith(".pdf");
            if (!pdf) {
                throw new IllegalArgumentException("Only PDF documents are supported in v1.");
            }
        }
    }

    private AnalysisMode resolveRequestedMode(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return AnalysisMode.SYNC;
        }
        try {
            return AnalysisMode.valueOf(rawMode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("mode must be either SYNC or ASYNC");
        }
    }

    private boolean hasAllRequiredSyncMetrics(StockScreeningRequestForm form) {
        Map<String, Object> manualMetrics = buildManualMetrics(form);
        return REQUIRED_SYNC_METRICS.stream().allMatch(manualMetrics::containsKey);
    }

    private Map<String, Object> buildManualMetrics(StockScreeningRequestForm form) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        putIfPresent(metrics, "interestBearingDebt", form.getInterestBearingDebt());
        putIfPresent(metrics, "cashAndInterestBearingAssets", form.getCashAndInterestBearingAssets());
        putIfPresent(metrics, "totalAssetsOrMarketCap", form.getTotalAssetsOrMarketCap());
        putIfPresent(metrics, "totalRevenue", form.getTotalRevenue());
        putIfPresent(metrics, "nonCompliantIncome", form.getNonCompliantIncome());
        putIfPresent(metrics, "sharesOwned", form.getSharesOwned());
        putIfPresent(metrics, "investedAmount", form.getInvestedAmount());
        return metrics;
    }

    private void putIfPresent(Map<String, Object> metrics, String key, BigDecimal value) {
        if (value != null) {
            metrics.put(key, value);
        }
    }

    private StockScreeningResponse toResponse(StockScreeningRequest request, StockScreeningResult result) {
        return StockScreeningResponse.builder()
                .requestId(request.getId())
                .status(request.getStatus())
                .verdict(result != null ? result.getVerdict() : null)
                .needsTatheer(result != null ? result.getNeedsTatheer() : null)
                .tatheerAmount(result != null ? result.getTatheerAmount() : null)
                .purificationRatio(result != null ? result.getPurificationRatio() : null)
                .purificationPerShare(result != null ? result.getPurificationPerShare() : null)
                .ruleResults(mapRuleResults(result))
                .metricsUsed(result != null ? result.getMetricsUsed() : null)
                .warnings(result != null ? result.getWarnings() : request.getWarnings())
                .userMessage(result != null ? result.getUserMessage() : null)
                .createdAt(request.getCreatedAt())
                .completedAt(request.getCompletedAt())
                .build();
    }

    private List<RuleResultResponse> mapRuleResults(StockScreeningResult result) {
        if (result == null || result.getRuleResults() == null) {
            return null;
        }

        return result.getRuleResults().stream()
                .map(rule -> RuleResultResponse.builder()
                        .ruleCode(String.valueOf(rule.get("ruleCode")))
                        .label(String.valueOf(rule.get("label")))
                        .status(String.valueOf(rule.get("status")))
                        .passed((Boolean) rule.get("passed"))
                        .message((String) rule.get("message"))
                        .details((Map<String, Object>) rule.get("details"))
                        .build())
                .toList();
    }

    private StockScreeningHistoryItemResponse toHistoryItem(StockScreeningRequest request) {
        StockScreeningResult result = request.getResult();
        return StockScreeningHistoryItemResponse.builder()
                .requestId(request.getId())
                .companyName(request.getCompanyName())
                .ticker(request.getTicker())
                .status(request.getStatus())
                .verdict(result != null ? result.getVerdict() : null)
                .needsTatheer(result != null ? result.getNeedsTatheer() : null)
                .tatheerAmount(result != null ? result.getTatheerAmount() : null)
                .purificationRatio(result != null ? result.getPurificationRatio() : null)
                .userMessage(resolveHistoryUserMessage(request, result))
                .createdAt(request.getCreatedAt())
                .completedAt(request.getCompletedAt())
                .requestedMode(request.getRequestedMode())
                .build();
    }

    private String resolveHistoryUserMessage(StockScreeningRequest request, StockScreeningResult result) {
        if (result != null && result.getUserMessage() != null && !result.getUserMessage().isBlank()) {
            return result.getUserMessage();
        }
        if (request.getWarnings() != null && !request.getWarnings().isEmpty()) {
            return request.getWarnings().getFirst();
        }
        return request.getProcessingError();
    }
}
