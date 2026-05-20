package com.example.mely.personal.coach;

import java.util.List;
import java.util.UUID;

import com.example.mely.model.HawlTracking;
import com.example.mely.model.ZakatAsset;
import com.example.mely.model.ZakatCalculation;
import com.example.mely.personal.dto.MoneySummaryResponse;
import com.example.mely.personal.dto.SavingsGoalApiResponse;
import com.example.mely.personal.model.PersonalBudget;
import com.example.mely.personal.model.PersonalTransaction;
import com.example.mely.stocks.model.StockScreeningRequest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FinanceCoachContext {
    private UUID userId;
    private String yearMonth;
    private MoneySummaryResponse monthlySummary;
    private List<PersonalBudget> budgets;
    private List<SavingsGoalApiResponse> goals;
    private List<ZakatAsset> zakatAssets;
    private HawlTracking hawl;
    private ZakatCalculation latestZakatCalculation;
    private List<StockScreeningRequest> recentScreenings;
    private List<PersonalTransaction> recentTransactions;
}
