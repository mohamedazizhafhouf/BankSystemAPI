package com.example.mely.stocks.service.impl;

import com.example.mely.stocks.model.StockScreeningDocument;
import com.example.mely.stocks.model.StockScreeningRequest;
import com.example.mely.stocks.service.FinancialMetricsExtractor;
import com.example.mely.stocks.service.analysis.ExtractionResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class PlaceholderFinancialMetricsExtractor implements FinancialMetricsExtractor {

    @Override
    public ExtractionResult extract(StockScreeningRequest request, List<StockScreeningDocument> documents) {
        List<String> warnings = new ArrayList<>();
        if (!documents.isEmpty()) {
            warnings.add("PDF extraction is using a placeholder adapter. Manual financial inputs remain authoritative until the LLM extractor is connected.");
        }
        return ExtractionResult.builder()
                .metrics(new LinkedHashMap<>())
                .warnings(warnings)
                .build();
    }
}
