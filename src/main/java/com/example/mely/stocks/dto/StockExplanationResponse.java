package com.example.mely.stocks.dto;

import com.example.mely.stocks.model.StockScreeningStatus;
import com.example.mely.stocks.model.StockVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockExplanationResponse {
    private UUID requestId;
    private StockScreeningStatus status;
    private StockVerdict verdict;
    private String explanation;
    private String userMessage;
    private List<String> warnings;
    private LocalDateTime completedAt;
}
