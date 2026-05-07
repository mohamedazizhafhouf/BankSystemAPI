package com.example.mely.personal.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.mely.personal.dto.MoneySummaryResponse;
import com.example.mely.personal.dto.PersonalBudgetRequest;
import com.example.mely.personal.dto.PersonalTransactionRequest;
import com.example.mely.personal.dto.SavingsGoalApiResponse;
import com.example.mely.personal.dto.SavingsGoalRequest;
import com.example.mely.personal.dto.SavingsSimulationRequest;
import com.example.mely.personal.dto.SavingsSimulationResponse;
import com.example.mely.personal.model.PersonalBudget;
import com.example.mely.personal.model.PersonalTransaction;
import com.example.mely.personal.service.PersonalFinanceService;
import com.example.mely.personal.service.WealthSimulationService;
import com.example.mely.security.AuthenticatedUser;
import com.example.mely.security.CurrentAuthenticatedUserProvider;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/personal/money")
@RequiredArgsConstructor
public class PersonalMoneyController {

    private final CurrentAuthenticatedUserProvider currentUser;
    private final PersonalFinanceService personalFinanceService;
    private final WealthSimulationService wealthSimulationService;

    @GetMapping("/transactions")
    public List<PersonalTransaction> listTransactions(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        UUID userId = requireSaas().getUserId();
        return personalFinanceService.listTransactions(userId, from, to);
    }

    @PostMapping("/transactions")
    public PersonalTransaction addTransaction(@Valid @RequestBody PersonalTransactionRequest request) {
        UUID userId = requireSaas().getUserId();
        return personalFinanceService.createTransaction(userId, request);
    }

    @DeleteMapping("/transactions/{id}")
    public void deleteTransaction(@PathVariable UUID id) {
        UUID userId = requireSaas().getUserId();
        personalFinanceService.deleteTransaction(userId, id);
    }

    @GetMapping("/budgets")
    public List<PersonalBudget> listBudgets(@RequestParam String yearMonth) {
        UUID userId = requireSaas().getUserId();
        return personalFinanceService.listBudgets(userId, yearMonth);
    }

    @PostMapping("/budgets")
    public PersonalBudget upsertBudget(@Valid @RequestBody PersonalBudgetRequest request) {
        UUID userId = requireSaas().getUserId();
        return personalFinanceService.upsertBudget(userId, request);
    }

    @DeleteMapping("/budgets/{id}")
    public void deleteBudget(@PathVariable UUID id) {
        UUID userId = requireSaas().getUserId();
        personalFinanceService.deleteBudget(userId, id);
    }

    @GetMapping("/summary")
    public MoneySummaryResponse summary(@RequestParam String yearMonth) {
        UUID userId = requireSaas().getUserId();
        return personalFinanceService.monthlySummary(userId, yearMonth);
    }

    @GetMapping("/goals")
    public List<SavingsGoalApiResponse> listGoals() {
        UUID userId = requireSaas().getUserId();
        return personalFinanceService.listGoals(userId);
    }

    @PostMapping("/goals")
    public SavingsGoalApiResponse createGoal(@Valid @RequestBody SavingsGoalRequest request) {
        UUID userId = requireSaas().getUserId();
        return personalFinanceService.createGoal(userId, request);
    }

    @PutMapping("/goals/{id}")
    public SavingsGoalApiResponse updateGoal(@PathVariable UUID id, @Valid @RequestBody SavingsGoalRequest request) {
        UUID userId = requireSaas().getUserId();
        return personalFinanceService.updateGoal(userId, id, request);
    }

    @DeleteMapping("/goals/{id}")
    public void deleteGoal(@PathVariable UUID id) {
        UUID userId = requireSaas().getUserId();
        personalFinanceService.deleteGoal(userId, id);
    }

    @PostMapping("/simulate")
    public SavingsSimulationResponse simulate(@Valid @RequestBody SavingsSimulationRequest request) {
        requireSaas();
        return wealthSimulationService.simulate(request);
    }

    private AuthenticatedUser requireSaas() {
        AuthenticatedUser u = currentUser.getCurrentUser();
        if (!"SAAS".equals(u.getAccountType())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Wealth hub is available for individual (SAAS) accounts.");
        }
        return u;
    }
}
