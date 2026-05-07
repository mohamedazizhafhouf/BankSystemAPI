package com.example.mely.personal.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.mely.personal.dto.MoneySummaryResponse;
import com.example.mely.personal.dto.PersonalBudgetRequest;
import com.example.mely.personal.dto.PersonalTransactionRequest;
import com.example.mely.personal.dto.SavingsGoalApiResponse;
import com.example.mely.personal.dto.SavingsGoalRequest;
import com.example.mely.personal.model.PersonalBudget;
import com.example.mely.personal.model.PersonalTransaction;
import com.example.mely.personal.model.PersonalTransactionDirection;
import com.example.mely.personal.model.SavingsGoal;
import com.example.mely.personal.repository.PersonalBudgetRepository;
import com.example.mely.personal.repository.PersonalTransactionRepository;
import com.example.mely.personal.repository.SavingsGoalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonalFinanceService {

    private final PersonalTransactionRepository transactionRepository;
    private final PersonalBudgetRepository budgetRepository;
    private final SavingsGoalRepository goalRepository;

    public List<PersonalTransaction> listTransactions(UUID userId, LocalDate from, LocalDate to) {
        return transactionRepository.findByUserIdAndOccurredOnBetweenOrderByOccurredOnDesc(userId, from, to);
    }

    @Transactional
    public PersonalTransaction createTransaction(UUID userId, PersonalTransactionRequest req) {
        PersonalTransaction tx = PersonalTransaction.builder()
                .userId(userId)
                .occurredOn(req.getOccurredOn())
                .direction(req.getDirection())
                .categoryCode(req.getCategoryCode().trim().toUpperCase())
                .amount(req.getAmount().setScale(4, RoundingMode.HALF_UP))
                .memo(req.getMemo())
                .build();
        return transactionRepository.save(tx);
    }

    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        PersonalTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (!tx.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found");
        }
        transactionRepository.delete(tx);
    }

    public List<PersonalBudget> listBudgets(UUID userId, String yearMonth) {
        return budgetRepository.findByUserIdAndYearMonth(userId, yearMonth);
    }

    @Transactional
    public PersonalBudget upsertBudget(UUID userId, PersonalBudgetRequest req) {
        String ym = req.getYearMonth();
        String cat = req.getCategoryCode().trim().toUpperCase();
        return budgetRepository.findByUserIdAndYearMonthAndCategoryCode(userId, ym, cat)
                .map(existing -> {
                    existing.setCapAmount(req.getCapAmount().setScale(4, RoundingMode.HALF_UP));
                    return budgetRepository.save(existing);
                })
                .orElseGet(() -> budgetRepository.save(PersonalBudget.builder()
                        .userId(userId)
                        .yearMonth(ym)
                        .categoryCode(cat)
                        .capAmount(req.getCapAmount().setScale(4, RoundingMode.HALF_UP))
                        .build()));
    }

    @Transactional
    public void deleteBudget(UUID userId, UUID budgetId) {
        PersonalBudget b = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
        if (!b.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found");
        }
        budgetRepository.delete(b);
    }

    public List<SavingsGoalApiResponse> listGoals(UUID userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(SavingsGoalApiResponse::from)
                .toList();
    }

    @Transactional
    public SavingsGoalApiResponse createGoal(UUID userId, SavingsGoalRequest req) {
        SavingsGoal g = SavingsGoal.builder()
                .userId(userId)
                .title(req.getTitle().trim())
                .targetAmount(req.getTargetAmount().setScale(4, RoundingMode.HALF_UP))
                .currentAmount(req.getCurrentAmount().setScale(4, RoundingMode.HALF_UP))
                .targetDate(req.getTargetDate())
                .build();
        return SavingsGoalApiResponse.from(goalRepository.save(g));
    }

    @Transactional
    public SavingsGoalApiResponse updateGoal(UUID userId, UUID goalId, SavingsGoalRequest req) {
        SavingsGoal g = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));
        if (!g.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found");
        }
        g.setTitle(req.getTitle().trim());
        g.setTargetAmount(req.getTargetAmount().setScale(4, RoundingMode.HALF_UP));
        g.setCurrentAmount(req.getCurrentAmount().setScale(4, RoundingMode.HALF_UP));
        g.setTargetDate(req.getTargetDate());
        return SavingsGoalApiResponse.from(goalRepository.save(g));
    }

    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        SavingsGoal g = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));
        if (!g.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found");
        }
        goalRepository.delete(g);
    }

    public MoneySummaryResponse monthlySummary(UUID userId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<PersonalTransaction> txs =
                transactionRepository.findByUserIdAndOccurredOnBetweenOrderByOccurredOnDesc(userId, from, to);

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        Map<String, BigDecimal> expenseByCat = new HashMap<>();

        for (PersonalTransaction t : txs) {
            if (t.getDirection() == PersonalTransactionDirection.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
                expenseByCat.merge(t.getCategoryCode(), t.getAmount(), BigDecimal::add);
            }
        }

        List<PersonalBudget> budgets = budgetRepository.findByUserIdAndYearMonth(userId, yearMonth);
        Map<String, BigDecimal> capByCat = new HashMap<>();
        for (PersonalBudget b : budgets) {
            capByCat.put(b.getCategoryCode(), b.getCapAmount());
        }

        List<MoneySummaryResponse.CategorySpendRow> rows = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : expenseByCat.entrySet()) {
            rows.add(MoneySummaryResponse.CategorySpendRow.builder()
                    .categoryCode(e.getKey())
                    .spent(e.getValue())
                    .budgetCap(capByCat.getOrDefault(e.getKey(), null))
                    .build());
        }
        rows.sort((a, b) -> a.getCategoryCode().compareTo(b.getCategoryCode()));

        BigDecimal net = income.subtract(expense).setScale(4, RoundingMode.HALF_UP);
        return MoneySummaryResponse.builder()
                .totalIncome(income.setScale(4, RoundingMode.HALF_UP))
                .totalExpense(expense.setScale(4, RoundingMode.HALF_UP))
                .netCashFlow(net)
                .expenseByCategory(rows)
                .build();
    }
}
