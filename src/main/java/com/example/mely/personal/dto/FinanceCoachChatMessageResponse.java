package com.example.mely.personal.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.mely.personal.model.FinanceCoachMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceCoachChatMessageResponse {
    private UUID id;
    private String role;
    private String content;
    private String yearMonth;
    private LocalDateTime createdAt;

    public static FinanceCoachChatMessageResponse from(FinanceCoachMessage message) {
        return FinanceCoachChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .yearMonth(message.getYearMonth())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
