package com.example.mely.personal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceCoachInsightCardResponse {
    private String type;
    private String severity;
    private String title;
    private String message;
    private String actionLabel;
    private String actionRoute;
}
