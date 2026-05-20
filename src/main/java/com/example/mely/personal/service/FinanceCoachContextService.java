package com.example.mely.personal.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.mely.personal.coach.FinanceCoachContext;
import com.example.mely.repository.ZakatCalculationRepository;
import com.example.mely.service.ZakatService;
import com.example.mely.stocks.repository.StockScreeningRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinanceCoachContextService {

    private final PersonalFinanceService personalFinanceService;
    private final ZakatService zakatService;
    private final ZakatCalculationRepository zakatCalculationRepository;
    private final StockScreeningRequestRepository stockScreeningRequestRepository;

    public FinanceCoachContext build(UUID userId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        return FinanceCoachContext.builder()
                .userId(userId)
                .yearMonth(yearMonth)
                .monthlySummary(personalFinanceService.monthlySummary(userId, yearMonth))
                .budgets(personalFinanceService.listBudgets(userId, yearMonth))
                .goals(personalFinanceService.listGoals(userId))
                .zakatAssets(zakatService.listActiveZakatAssets(userId))
                .hawl(zakatService.findHawlTracking(userId).orElse(null))
                .latestZakatCalculation(zakatCalculationRepository.findTopByUserIdOrderByCalculationDateDesc(userId).orElse(null))
                .recentScreenings(stockScreeningRequestRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5)))
                .recentTransactions(personalFinanceService.listTransactions(userId, from, to).stream().limit(8).toList())
                .build();
    }
}
