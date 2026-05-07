package com.example.mely.personal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.mely.personal.model.SavingsGoal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SavingsGoalApiResponse {
    UUID id;
    String title;
    BigDecimal targetAmount;
    BigDecimal currentAmount;
    LocalDate targetDate;
    LocalDateTime createdAt;

    public static SavingsGoalApiResponse from(SavingsGoal g) {
        return SavingsGoalApiResponse.builder()
                .id(g.getId())
                .title(g.getTitle())
                .targetAmount(g.getTargetAmount())
                .currentAmount(g.getCurrentAmount())
                .targetDate(g.getTargetDate())
                .createdAt(g.getCreatedAt())
                .build();
    }
}
