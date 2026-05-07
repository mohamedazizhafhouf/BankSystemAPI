package com.example.mely.stocks.dto;

import com.example.mely.stocks.model.AnalysisMode;
import com.example.mely.stocks.model.StockScreeningStatus;
import com.example.mely.stocks.model.StockScreeningMode;
import com.example.mely.stocks.model.StockVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockScreeningHistoryItemResponse {
    private UUID requestId;
    private String companyName;
    private String ticker;
    private StockScreeningStatus status;
    private StockVerdict verdict;
    private Boolean needsTatheer;
    private BigDecimal tatheerAmount;
    private BigDecimal purificationRatio;
    private String userMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private AnalysisMode requestedMode;
    private StockScreeningMode screeningMode;
}
