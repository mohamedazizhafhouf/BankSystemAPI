package com.example.mely.personal.coach;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.mely.config.FinanceCoachProperties;
import com.example.mely.personal.dto.FinanceCoachChatMessageResponse;
import com.example.mely.personal.dto.FinanceCoachInsightsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProviderAgnosticFinanceCoachLlmClient implements FinanceCoachLlmClient {

    private final FinanceCoachProperties properties;
    private final GroqFinanceCoachClient groqClient;
    private final DeterministicFinanceCoachAdvisor fallbackAdvisor;

    @Override
    public FinanceCoachInsightsResponse generateMonthlyInsights(FinanceCoachContext context) {
        if (useGroq()) {
            try {
                return groqClient.generateMonthlyInsights(context);
            } catch (RuntimeException ignored) {
                return fallbackAdvisor.generateMonthlyInsights(context);
            }
        }
        return fallbackAdvisor.generateMonthlyInsights(context);
    }

    @Override
    public String replyToChat(
            FinanceCoachContext context,
            List<FinanceCoachChatMessageResponse> history,
            String userMessage) {
        if (useGroq()) {
            try {
                return groqClient.replyToChat(context, history, userMessage);
            } catch (RuntimeException ignored) {
                return fallbackAdvisor.replyToChat(context, userMessage);
            }
        }
        return fallbackAdvisor.replyToChat(context, userMessage);
    }

    @Override
    public String providerName() {
        if (useGroq()) {
            return "groq";
        }
        return "deterministic";
    }

    private boolean useGroq() {
        return "groq".equalsIgnoreCase(properties.getProvider()) && groqClient.isConfigured();
    }
}
