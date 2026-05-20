package com.example.mely.personal.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceCoachInsightsResponse {
    private String yearMonth;
    private String summary;
    private Integer healthScore;
    private List<FinanceCoachInsightCardResponse> cards;
    private LocalDateTime generatedAt;
}
