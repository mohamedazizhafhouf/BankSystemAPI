package com.example.mely.stocks.dto;

import com.example.mely.stocks.model.StockScreeningStatus;
import com.example.mely.stocks.model.StockVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockScreeningResponse {
    private UUID requestId;
    private StockScreeningStatus status;
    private StockVerdict verdict;
    private Boolean needsTatheer;
    private BigDecimal tatheerAmount;
    private BigDecimal purificationRatio;
    private BigDecimal purificationPerShare;
    private List<RuleResultResponse> ruleResults;
    private Map<String, Object> metricsUsed;
    private List<String> warnings;
    private String userMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
