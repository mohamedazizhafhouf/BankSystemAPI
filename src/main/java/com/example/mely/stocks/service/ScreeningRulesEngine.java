package com.example.mely.stocks.service;

import com.example.mely.stocks.model.RuleProfile;
import com.example.mely.stocks.service.analysis.ActivityAnalysisResult;
import com.example.mely.stocks.service.analysis.ResolvedMetric;
import com.example.mely.stocks.service.analysis.ScreeningComputation;

import java.util.Map;

public interface ScreeningRulesEngine {
    ScreeningComputation evaluate(RuleProfile ruleProfile,
                                  Map<String, ResolvedMetric> metrics,
                                  ActivityAnalysisResult activityAnalysisResult);
}
