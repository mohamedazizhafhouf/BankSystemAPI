package com.example.mely.stocks.dto;

import com.example.mely.stocks.model.StockScreeningStatus;
import com.example.mely.stocks.model.StockVerdict;
import com.example.mely.stocks.model.StockScreeningMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockScreeningResponse {
    private UUID requestId;
    private StockScreeningMode screeningMode;
    private StockScreeningStatus status;
    private String companyName;
    private String ticker;
    private StockVerdict verdict;
    private Boolean needsTatheer;
    private BigDecimal tatheerAmount;
    private BigDecimal purificationRatio;
    private BigDecimal purificationPerShare;
    private List<RuleResultResponse> ruleResults;
    private Map<String, Object> metricsUsed;
    private List<String> warnings;
    private String userMessage;
    private String explanation;
    private String activitySummary;
    private List<StockDocumentResponse> documents;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
