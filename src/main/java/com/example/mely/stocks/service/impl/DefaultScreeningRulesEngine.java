package com.example.mely.stocks.service.impl;

import com.example.mely.stocks.model.RuleProfile;
import com.example.mely.stocks.model.StockVerdict;
import com.example.mely.stocks.service.ScreeningRulesEngine;
import com.example.mely.stocks.service.analysis.ActivityAnalysisResult;
import com.example.mely.stocks.service.analysis.ResolvedMetric;
import com.example.mely.stocks.service.analysis.ScreeningComputation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultScreeningRulesEngine implements ScreeningRulesEngine {

    @Override
    public ScreeningComputation evaluate(RuleProfile ruleProfile,
                                         Map<String, ResolvedMetric> metrics,
                                         ActivityAnalysisResult activityAnalysisResult) {
        List<Map<String, Object>> ruleResults = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        boolean missingRequiredMetrics = false;
        boolean anyFailedRatio = false;

        BigDecimal debtRatioThreshold = decimalThreshold(ruleProfile, "debtRatioThreshold");
        BigDecimal cashRatioThreshold = decimalThreshold(ruleProfile, "cashRatioThreshold");
        BigDecimal nonCompliantIncomeThreshold = decimalThreshold(ruleProfile, "nonCompliantIncomeThreshold");

        Map<String, Object> metricsUsed = new LinkedHashMap<>();
        metrics.forEach((key, value) -> metricsUsed.put(key, value.toResponseMap()));

        Map<String, Object> activityRule = baseRule("activity-screen", "Business activity screening");
        if (activityAnalysisResult.isCompliant()) {
            passRule(activityRule, activityAnalysisResult.getRationale());
        } else {
            failRule(activityRule, activityAnalysisResult.getRationale());
        }
        if (activityAnalysisResult.getMatchedKeyword() != null) {
            activityRule.put("details", Map.of("matchedKeyword", activityAnalysisResult.getMatchedKeyword()));
        }
        ruleResults.add(activityRule);

        RatioOutcome debtRatio = computeRatio(metrics, "interestBearingDebt", "totalAssetsOrMarketCap");
        ruleResults.add(createRatioRule(
                "debt-ratio",
                "Interest-bearing debt ratio",
                debtRatio,
                debtRatioThreshold,
                "interestBearingDebt / totalAssetsOrMarketCap",
                warnings
        ));
        missingRequiredMetrics |= debtRatio.missing();
        anyFailedRatio |= debtRatio.failed(debtRatioThreshold);

        RatioOutcome cashRatio = computeRatio(metrics, "cashAndInterestBearingAssets", "totalAssetsOrMarketCap");
        ruleResults.add(createRatioRule(
                "cash-ratio",
                "Interest-bearing cash/assets ratio",
                cashRatio,
                cashRatioThreshold,
                "cashAndInterestBearingAssets / totalAssetsOrMarketCap",
                warnings
        ));
        missingRequiredMetrics |= cashRatio.missing();
        anyFailedRatio |= cashRatio.failed(cashRatioThreshold);

        RatioOutcome nonCompliantIncomeRatio = computeRatio(metrics, "nonCompliantIncome", "totalRevenue");
        Map<String, Object> incomeRule = createRatioRule(
                "non-compliant-income-ratio",
                "Non-compliant income ratio",
                nonCompliantIncomeRatio,
                nonCompliantIncomeThreshold,
                "nonCompliantIncome / totalRevenue",
                warnings
        );
        if (!nonCompliantIncomeRatio.missing() && nonCompliantIncomeRatio.actual().compareTo(BigDecimal.ZERO) > 0) {
            incomeRule.put("purificationFlag", true);
        }
        ruleResults.add(incomeRule);
        missingRequiredMetrics |= nonCompliantIncomeRatio.missing();
        anyFailedRatio |= nonCompliantIncomeRatio.failed(nonCompliantIncomeThreshold);

        StockVerdict verdict;
        boolean halalEligible;
        if (!activityAnalysisResult.isCompliant() || anyFailedRatio) {
            verdict = StockVerdict.NON_HALAL;
            halalEligible = false;
        } else if (missingRequiredMetrics) {
            verdict = StockVerdict.REVIEW_REQUIRED;
            halalEligible = false;
            warnings.add("Some financial metrics are missing, so the system could not produce a fully confident Sharia verdict.");
        } else {
            verdict = StockVerdict.HALAL;
            halalEligible = true;
        }

        return ScreeningComputation.builder()
                .verdict(verdict)
                .ruleResults(ruleResults)
                .warnings(warnings)
                .metricsUsed(metricsUsed)
                .halalEligible(halalEligible)
                .build();
    }

    private Map<String, Object> createRatioRule(String ruleCode,
                                                String label,
                                                RatioOutcome ratioOutcome,
                                                BigDecimal threshold,
                                                String formula,
                                                List<String> warnings) {
        Map<String, Object> rule = baseRule(ruleCode, label);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("formula", formula);
        details.put("threshold", threshold);

        if (ratioOutcome.missing()) {
            rule.put("status", "REVIEW_REQUIRED");
            rule.put("passed", null);
            rule.put("message", "Required values are missing for this ratio.");
            details.put("actualValue", null);
            warnings.add("Missing inputs for " + label + ".");
        } else if (ratioOutcome.failed(threshold)) {
            failRule(rule, label + " exceeds the configured threshold.");
            details.put("actualValue", ratioOutcome.actual());
        } else {
            passRule(rule, label + " is within the configured threshold.");
            details.put("actualValue", ratioOutcome.actual());
        }

        rule.put("details", details);
        return rule;
    }

    private RatioOutcome computeRatio(Map<String, ResolvedMetric> metrics, String numeratorKey, String denominatorKey) {
        BigDecimal numerator = metricValue(metrics, numeratorKey);
        BigDecimal denominator = metricValue(metrics, denominatorKey);
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return new RatioOutcome(null, true);
        }
        return new RatioOutcome(numerator.divide(denominator, 6, RoundingMode.HALF_UP), false);
    }

    private BigDecimal metricValue(Map<String, ResolvedMetric> metrics, String key) {
        ResolvedMetric metric = metrics.get(key);
        return metric != null ? metric.getValue() : null;
    }

    private BigDecimal decimalThreshold(RuleProfile ruleProfile, String key) {
        Object raw = ruleProfile.getThresholds().get(key);
        if (raw == null) {
            throw new IllegalStateException("Missing threshold '" + key + "' in active rule profile");
        }
        return new BigDecimal(raw.toString());
    }

    private Map<String, Object> baseRule(String ruleCode, String label) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("ruleCode", ruleCode);
        rule.put("label", label);
        return rule;
    }

    private void passRule(Map<String, Object> rule, String message) {
        rule.put("status", "PASS");
        rule.put("passed", true);
        rule.put("message", message);
    }

    private void failRule(Map<String, Object> rule, String message) {
        rule.put("status", "FAIL");
        rule.put("passed", false);
        rule.put("message", message);
    }

    private record RatioOutcome(BigDecimal actual, boolean missing) {
        boolean failed(BigDecimal threshold) {
            return !missing && actual.compareTo(threshold) > 0;
        }
    }
}
