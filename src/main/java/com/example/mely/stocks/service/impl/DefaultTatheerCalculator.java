package com.example.mely.stocks.service.impl;

import com.example.mely.stocks.service.TatheerCalculator;
import com.example.mely.stocks.service.analysis.ResolvedMetric;
import com.example.mely.stocks.service.analysis.TatheerComputation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class DefaultTatheerCalculator implements TatheerCalculator {

    @Override
    public TatheerComputation calculate(Map<String, ResolvedMetric> metrics, boolean halalEligible) {
        BigDecimal nonCompliantIncome = getMetricValue(metrics, "nonCompliantIncome");
        BigDecimal totalRevenue = getMetricValue(metrics, "totalRevenue");
        BigDecimal investedAmount = getMetricValue(metrics, "investedAmount");
        BigDecimal sharesOwned = getMetricValue(metrics, "sharesOwned");
        BigDecimal sharesOutstanding = getMetricValue(metrics, "sharesOutstanding");

        BigDecimal purificationRatio = null;
        if (nonCompliantIncome != null && totalRevenue != null && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            purificationRatio = nonCompliantIncome.divide(totalRevenue, 6, RoundingMode.HALF_UP);
        }

        BigDecimal purificationPerShare = null;
        if (nonCompliantIncome != null && sharesOutstanding != null && sharesOutstanding.compareTo(BigDecimal.ZERO) > 0) {
            purificationPerShare = nonCompliantIncome.divide(sharesOutstanding, 6, RoundingMode.HALF_UP);
        }

        BigDecimal tatheerAmount = null;
        if (halalEligible) {
            if (investedAmount != null && purificationRatio != null) {
                tatheerAmount = investedAmount.multiply(purificationRatio).setScale(6, RoundingMode.HALF_UP);
            } else if (sharesOwned != null && purificationPerShare != null) {
                tatheerAmount = sharesOwned.multiply(purificationPerShare).setScale(6, RoundingMode.HALF_UP);
            }
        }

        boolean needsTatheer = halalEligible
                && purificationRatio != null
                && purificationRatio.compareTo(BigDecimal.ZERO) > 0;

        return TatheerComputation.builder()
                .needsTatheer(needsTatheer)
                .tatheerAmount(tatheerAmount)
                .purificationRatio(purificationRatio)
                .purificationPerShare(purificationPerShare)
                .build();
    }

    private BigDecimal getMetricValue(Map<String, ResolvedMetric> metrics, String key) {
        ResolvedMetric metric = metrics.get(key);
        return metric != null ? metric.getValue() : null;
    }
}
