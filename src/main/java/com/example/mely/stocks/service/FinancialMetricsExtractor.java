package com.example.mely.stocks.service;

import com.example.mely.stocks.model.StockScreeningDocument;
import com.example.mely.stocks.model.StockScreeningRequest;
import com.example.mely.stocks.service.analysis.ExtractionResult;

import java.util.List;

public interface FinancialMetricsExtractor {
    ExtractionResult extract(StockScreeningRequest request, List<StockScreeningDocument> documents);
}
