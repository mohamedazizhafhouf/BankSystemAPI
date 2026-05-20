package com.example.mely.personal.coach;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.mely.config.FinanceCoachProperties;
import com.example.mely.personal.dto.FinanceCoachChatMessageResponse;
import com.example.mely.personal.dto.FinanceCoachInsightCardResponse;
import com.example.mely.personal.dto.FinanceCoachInsightsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GroqFinanceCoachClient {

    private final FinanceCoachProperties properties;
    private final ObjectMapper objectMapper;

    public boolean isConfigured() {
        return properties.getGroq().getApiKey() != null && !properties.getGroq().getApiKey().isBlank();
    }

    public FinanceCoachInsightsResponse generateMonthlyInsights(FinanceCoachContext context) {
        String prompt = """
                Generate Islamic personal finance coach insights as strict JSON.
                Output exactly:
                {
                  "summary": "one short paragraph",
                  "healthScore": 0-100,
                  "cards": [
                    {
                      "type": "BUDGET_ALERT|ZAKAT|HAWL|SAVINGS_GOAL|HALAL_INVESTING|CASHFLOW|ONBOARDING",
                      "severity": "GOOD|INFO|WARN|DANGER",
                      "title": "short title",
                      "message": "specific helpful message",
                      "actionLabel": "short action",
                      "actionRoute": "/wealth or /halal-stocks/history or /zakat"
                    }
                  ]
                }

                Stay educational. Do not issue fatwas. Do not recommend buying or selling securities.

                User context:
                %s
                """.formatted(toJson(context));

        String content = completeText(List.of(
                message("system", "You are Mely, an Islamic personal finance coach. Return only valid JSON when asked for JSON."),
                message("user", prompt)));
        return parseInsights(content, context.getYearMonth());
    }

    public String replyToChat(
            FinanceCoachContext context,
            List<FinanceCoachChatMessageResponse> history,
            String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message(
                "system",
                """
                        You are Mely, an Islamic finance coach for personal money management.
                        Ground answers in the provided user context and conversation history.
                        Focus on budgeting, savings, zakat, hawl, nisab, halal investing, and purification.
                        Do not give fatwa-level certainty, tax/legal advice, or buy/sell instructions.
                        Keep replies concise, practical, and educational.
                        """));
        messages.add(message("user", "Current user context:\n" + toJson(context)));
        for (FinanceCoachChatMessageResponse item : history.stream().skip(Math.max(0, history.size() - 8)).toList()) {
            messages.add(message("ASSISTANT".equals(item.getRole()) ? "assistant" : "user", item.getContent()));
        }
        messages.add(message("user", userMessage));
        return completeText(messages);
    }

    private String completeText(List<Map<String, String>> messages) {
        FinanceCoachProperties.Groq groq = properties.getGroq();
        RestClient restClient = RestClient.builder()
                .baseUrl(groq.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + groq.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();

        Map<String, Object> body = Map.of(
                "model", groq.getModel(),
                "temperature", 0.2,
                "messages", messages);

        try {
            String raw = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("Groq returned an empty response.");
            }
            return content.asText();
        } catch (RestClientException | JsonProcessingException ex) {
            throw new IllegalStateException("Groq finance coach request failed: " + ex.getMessage(), ex);
        }
    }

    private FinanceCoachInsightsResponse parseInsights(String content, String yearMonth) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(content));
            List<FinanceCoachInsightCardResponse> cards = objectMapper.convertValue(
                    root.path("cards"),
                    new TypeReference<List<FinanceCoachInsightCardResponse>>() {});
            return FinanceCoachInsightsResponse.builder()
                    .yearMonth(yearMonth)
                    .summary(root.path("summary").asText("Mely prepared your Islamic finance guidance."))
                    .healthScore(root.path("healthScore").asInt(70))
                    .cards(cards == null ? List.of() : cards.stream().limit(5).toList())
                    .build();
        } catch (RuntimeException | JsonProcessingException ex) {
            throw new IllegalStateException("Groq finance coach returned invalid insight JSON.", ex);
        }
    }

    private Map<String, String> message(String role, String content) {
        return Map.of("role", role, "content", content);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String stripCodeFence(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        return trimmed
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
    }
}
