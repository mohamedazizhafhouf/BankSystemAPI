package com.example.mely.stocks.service;

import com.example.mely.stocks.model.StockScreeningRequest;
import com.example.mely.stocks.service.analysis.ActivityAnalysisResult;
import com.example.mely.stocks.service.analysis.ScreeningComputation;

public interface ExplanationComposer {
    String composeUserMessage(StockScreeningRequest request, ScreeningComputation computation, ActivityAnalysisResult activityAnalysisResult);

    String composeDetailedExplanation(StockScreeningRequest request, ScreeningComputation computation, ActivityAnalysisResult activityAnalysisResult);
}
