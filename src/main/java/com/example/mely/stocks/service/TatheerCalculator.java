package com.example.mely.stocks.service;

import com.example.mely.stocks.service.analysis.ResolvedMetric;
import com.example.mely.stocks.service.analysis.TatheerComputation;

import java.util.Map;

public interface TatheerCalculator {
    TatheerComputation calculate(Map<String, ResolvedMetric> metrics, boolean halalEligible);
}
