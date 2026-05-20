package com.example.mely.personal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceCoachChatResponse {
    private FinanceCoachChatMessageResponse assistantMessage;
}
