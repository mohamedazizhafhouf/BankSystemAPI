package com.example.mely.personal.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MoneySummaryResponse {
    BigDecimal totalIncome;
    BigDecimal totalExpense;
    BigDecimal netCashFlow;
    List<CategorySpendRow> expenseByCategory;

    @Value
    @Builder
    public static class CategorySpendRow {
        String categoryCode;
        BigDecimal spent;
        BigDecimal budgetCap;
    }
}
