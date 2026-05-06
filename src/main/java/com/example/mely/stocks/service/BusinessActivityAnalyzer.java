package com.example.mely.stocks.service;

import com.example.mely.stocks.model.RuleProfile;
import com.example.mely.stocks.model.StockScreeningRequest;
import com.example.mely.stocks.service.analysis.ActivityAnalysisResult;

public interface BusinessActivityAnalyzer {
    ActivityAnalysisResult analyze(StockScreeningRequest request, RuleProfile ruleProfile);
}
