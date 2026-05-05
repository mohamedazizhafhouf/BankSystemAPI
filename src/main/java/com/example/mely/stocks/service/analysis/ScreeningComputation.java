package com.example.mely.stocks.service.analysis;

import com.example.mely.stocks.model.StockVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningComputation {
    private StockVerdict verdict;
    private List<Map<String, Object>> ruleResults;
    private List<String> warnings;
    private Map<String, Object> metricsUsed;
    private boolean halalEligible;
}
