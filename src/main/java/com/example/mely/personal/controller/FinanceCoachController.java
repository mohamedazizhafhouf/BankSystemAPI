package com.example.mely.personal.controller;

import java.time.YearMonth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.mely.personal.dto.FinanceCoachChatHistoryResponse;
import com.example.mely.personal.dto.FinanceCoachChatRequest;
import com.example.mely.personal.dto.FinanceCoachChatResponse;
import com.example.mely.personal.dto.FinanceCoachInsightsResponse;
import com.example.mely.personal.service.FinanceCoachService;
import com.example.mely.security.AuthenticatedUser;
import com.example.mely.security.CurrentAuthenticatedUserProvider;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/personal/coach")
@RequiredArgsConstructor
public class FinanceCoachController {

    private final CurrentAuthenticatedUserProvider currentUser;
    private final FinanceCoachService coachService;

    @GetMapping("/monthly-insights")
    public FinanceCoachInsightsResponse monthlyInsights(@RequestParam(required = false) String yearMonth) {
        AuthenticatedUser user = requireSaas();
        return coachService.monthlyInsights(user.getUserId(), normalizeMonth(yearMonth));
    }

    @PostMapping("/chat")
    public FinanceCoachChatResponse chat(@Valid @RequestBody FinanceCoachChatRequest request) {
        AuthenticatedUser user = requireSaas();
        return coachService.chat(user.getUserId(), normalizeMonth(request.getYearMonth()), request.getMessage());
    }

    @GetMapping("/chat")
    public FinanceCoachChatHistoryResponse chatHistory() {
        AuthenticatedUser user = requireSaas();
        return coachService.chatHistory(user.getUserId());
    }

    private AuthenticatedUser requireSaas() {
        AuthenticatedUser user = currentUser.getCurrentUser();
        if (!"SAAS".equals(user.getAccountType())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "AI finance coach is available for individual (SAAS) accounts.");
        }
        return user;
    }

    private String normalizeMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return YearMonth.now().toString();
        }
        return YearMonth.parse(yearMonth).toString();
    }
}
