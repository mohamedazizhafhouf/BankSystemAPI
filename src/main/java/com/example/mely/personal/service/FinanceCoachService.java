package com.example.mely.personal.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.mely.personal.coach.FinanceCoachContext;
import com.example.mely.personal.coach.FinanceCoachLlmClient;
import com.example.mely.personal.dto.FinanceCoachChatHistoryResponse;
import com.example.mely.personal.dto.FinanceCoachChatMessageResponse;
import com.example.mely.personal.dto.FinanceCoachChatResponse;
import com.example.mely.personal.dto.FinanceCoachInsightCardResponse;
import com.example.mely.personal.dto.FinanceCoachInsightsResponse;
import com.example.mely.personal.model.FinanceCoachInsight;
import com.example.mely.personal.model.FinanceCoachMessage;
import com.example.mely.personal.repository.FinanceCoachInsightRepository;
import com.example.mely.personal.repository.FinanceCoachMessageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinanceCoachService {

    private final FinanceCoachContextService contextService;
    private final FinanceCoachLlmClient llmClient;
    private final FinanceCoachInsightRepository insightRepository;
    private final FinanceCoachMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public FinanceCoachInsightsResponse monthlyInsights(UUID userId, String yearMonth) {
        String normalizedMonth = normalizeMonth(yearMonth);
        return insightRepository.findByUserIdAndYearMonth(userId, normalizedMonth)
                .map(this::toResponse)
                .orElseGet(() -> generateAndSaveInsights(userId, normalizedMonth));
    }

    @Transactional
    public FinanceCoachChatResponse chat(UUID userId, String yearMonth, String message) {
        String normalizedMonth = normalizeMonth(yearMonth);
        FinanceCoachMessage userMessage = messageRepository.save(FinanceCoachMessage.builder()
                .userId(userId)
                .yearMonth(normalizedMonth)
                .role("USER")
                .content(message.trim())
                .build());

        FinanceCoachContext context = contextService.build(userId, normalizedMonth);
        List<FinanceCoachChatMessageResponse> history = messageRepository
                .findByUserIdAndYearMonthOrderByCreatedAtAsc(userId, normalizedMonth, PageRequest.of(0, 20))
                .stream()
                .map(FinanceCoachChatMessageResponse::from)
                .toList();
        String answer = llmClient.replyToChat(context, history, userMessage.getContent());
        FinanceCoachMessage assistant = messageRepository.save(FinanceCoachMessage.builder()
                .userId(userId)
                .yearMonth(normalizedMonth)
                .role("ASSISTANT")
                .content(answer)
                .build());

        return FinanceCoachChatResponse.builder()
                .assistantMessage(FinanceCoachChatMessageResponse.from(assistant))
                .build();
    }

    public FinanceCoachChatHistoryResponse chatHistory(UUID userId) {
        return FinanceCoachChatHistoryResponse.builder()
                .messages(messageRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                        .map(FinanceCoachChatMessageResponse::from)
                        .toList())
                .build();
    }

    private FinanceCoachInsightsResponse generateAndSaveInsights(UUID userId, String yearMonth) {
        FinanceCoachContext context = contextService.build(userId, yearMonth);
        FinanceCoachInsightsResponse generated = llmClient.generateMonthlyInsights(context);
        LocalDateTime generatedAt = LocalDateTime.now();
        FinanceCoachInsight saved = insightRepository.save(FinanceCoachInsight.builder()
                .userId(userId)
                .yearMonth(yearMonth)
                .healthScore(safeScore(generated.getHealthScore()))
                .summary(generated.getSummary())
                .cards(toCardMaps(generated.getCards()))
                .provider(llmClient.providerName())
                .generatedAt(generatedAt)
                .build());
        return toResponse(saved);
    }

    private FinanceCoachInsightsResponse toResponse(FinanceCoachInsight insight) {
        return FinanceCoachInsightsResponse.builder()
                .yearMonth(insight.getYearMonth())
                .summary(insight.getSummary())
                .healthScore(insight.getHealthScore())
                .cards(toCards(insight.getCards()))
                .generatedAt(insight.getGeneratedAt())
                .build();
    }

    private List<Map<String, Object>> toCardMaps(List<FinanceCoachInsightCardResponse> cards) {
        return objectMapper.convertValue(
                cards == null ? List.of() : cards,
                new TypeReference<List<Map<String, Object>>>() {});
    }

    private List<FinanceCoachInsightCardResponse> toCards(List<Map<String, Object>> cards) {
        return objectMapper.convertValue(
                cards == null ? List.of() : cards,
                new TypeReference<List<FinanceCoachInsightCardResponse>>() {});
    }

    private int safeScore(Integer score) {
        if (score == null) {
            return 70;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String normalizeMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return YearMonth.from(LocalDate.now()).toString();
        }
        return YearMonth.parse(yearMonth).toString();
    }
}
