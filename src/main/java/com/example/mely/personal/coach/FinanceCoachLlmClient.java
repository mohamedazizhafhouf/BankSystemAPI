package com.example.mely.personal.coach;

import java.util.List;

import com.example.mely.personal.dto.FinanceCoachChatMessageResponse;
import com.example.mely.personal.dto.FinanceCoachInsightsResponse;

public interface FinanceCoachLlmClient {

    FinanceCoachInsightsResponse generateMonthlyInsights(FinanceCoachContext context);

    String replyToChat(
            FinanceCoachContext context,
            List<FinanceCoachChatMessageResponse> history,
            String userMessage);

    String providerName();
}
