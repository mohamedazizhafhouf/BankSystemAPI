package com.example.mely.personal.coach;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.mely.personal.dto.FinanceCoachInsightCardResponse;
import com.example.mely.personal.dto.FinanceCoachInsightsResponse;
import com.example.mely.personal.dto.MoneySummaryResponse;
import com.example.mely.personal.model.PersonalBudget;

@Component
public class DeterministicFinanceCoachAdvisor {

    public FinanceCoachInsightsResponse generateMonthlyInsights(FinanceCoachContext context) {
        MoneySummaryResponse summary = context.getMonthlySummary();
        BigDecimal income = value(summary.getTotalIncome());
        BigDecimal expense = value(summary.getTotalExpense());
        BigDecimal net = value(summary.getNetCashFlow());

        List<FinanceCoachInsightCardResponse> cards = new ArrayList<>();
        int score = 72;

        if (income.compareTo(BigDecimal.ZERO) == 0 && expense.compareTo(BigDecimal.ZERO) == 0) {
            cards.add(card(
                    "ONBOARDING",
                    "INFO",
                    "Start with a few transactions",
                    "Add income and expenses so Mely can give finance guidance grounded in your real month.",
                    "Open wealth hub",
                    "/wealth"));
            score = 55;
        } else if (net.compareTo(BigDecimal.ZERO) >= 0) {
            cards.add(card(
                    "CASHFLOW",
                    "GOOD",
                    "Positive monthly cash flow",
                    "Your income is currently above expenses. Consider assigning part of the surplus to a savings goal or zakat-ready reserve.",
                    "Review goals",
                    "/wealth"));
            score += 10;
        } else {
            cards.add(card(
                    "CASHFLOW",
                    "WARN",
                    "Expenses are above income",
                    "This month is running negative. Review discretionary categories before adding new investment commitments.",
                    "Review cash flow",
                    "/wealth"));
            score -= 18;
        }

        addBudgetCards(context, cards);

        if (context.getZakatAssets().isEmpty()) {
            cards.add(card(
                    "ZAKAT",
                    "INFO",
                    "Zakat portfolio is empty",
                    "Add cash, gold, savings, or investment assets so Mely can track nisab and hawl readiness.",
                    "Add zakat assets",
                    "/wealth"));
            score -= 6;
        } else if (context.getHawl() == null) {
            cards.add(card(
                    "HAWL",
                    "WARN",
                    "Hawl tracking is not enabled",
                    "You already have zakatable assets. Enable tracking so Mely can monitor the lunar-year requirement.",
                    "Enable tracking",
                    "/wealth"));
            score -= 5;
        } else {
            String days = context.getHawl().getDaysRemaining() == null ? "unknown" : context.getHawl().getDaysRemaining().toString();
            cards.add(card(
                    "HAWL",
                    "INFO",
                    "Hawl status is visible",
                    "Mely can now track your lunar-year status. Days remaining: " + days + ".",
                    "View zakat portfolio",
                    "/wealth"));
        }

        if (!context.getRecentScreenings().isEmpty()) {
            cards.add(card(
                    "HALAL_INVESTING",
                    "INFO",
                    "Halal screening history is active",
                    "You have recent stock screenings. Revisit purification ratios before deciding how much income needs tatheer.",
                    "Open screening history",
                    "/halal-stocks/history"));
        }

        score = Math.max(0, Math.min(100, score));
        String monthSummary = buildSummary(income, expense, net, cards);
        return FinanceCoachInsightsResponse.builder()
                .yearMonth(context.getYearMonth())
                .summary(monthSummary)
                .healthScore(score)
                .cards(cards.stream().limit(5).toList())
                .build();
    }

    public String replyToChat(FinanceCoachContext context, String userMessage) {
        FinanceCoachInsightsResponse insights = generateMonthlyInsights(context);
        String question = userMessage == null ? "" : userMessage.trim();
        return """
                Based on your current Mely data for %s, your financial health score is %d/100.

                %s

                Your question: %s

                Practical next step: %s

                This is educational Islamic finance guidance, not a fatwa or personalized investment advice.
                """.formatted(
                context.getYearMonth(),
                insights.getHealthScore(),
                insights.getSummary(),
                question,
                insights.getCards().isEmpty() ? "Add more finance data so the coach can be more specific." : insights.getCards().get(0).getMessage());
    }

    private void addBudgetCards(FinanceCoachContext context, List<FinanceCoachInsightCardResponse> cards) {
        MoneySummaryResponse summary = context.getMonthlySummary();
        for (MoneySummaryResponse.CategorySpendRow row : summary.getExpenseByCategory()) {
            PersonalBudget budget = context.getBudgets().stream()
                    .filter(item -> item.getCategoryCode().equals(row.getCategoryCode()))
                    .findFirst()
                    .orElse(null);
            BigDecimal spent = value(row.getSpent());
            BigDecimal cap = budget == null ? value(row.getBudgetCap()) : budget.getCapAmount();
            if (cap == null || cap.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal usage = spent.divide(cap, 4, RoundingMode.HALF_UP);
            if (usage.compareTo(new BigDecimal("1.0")) >= 0) {
                cards.add(card(
                        "BUDGET_ALERT",
                        "WARN",
                        row.getCategoryCode() + " budget is over cap",
                        "You spent " + spent.stripTrailingZeros().toPlainString() + " against a cap of " + cap.stripTrailingZeros().toPlainString() + ".",
                        "Review budgets",
                        "/wealth"));
            } else if (usage.compareTo(new BigDecimal("0.8")) >= 0) {
                cards.add(card(
                        "BUDGET_ALERT",
                        "WARN",
                        row.getCategoryCode() + " budget is close to cap",
                        "You used about " + usage.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP) + "% of this category's monthly budget.",
                        "Review budgets",
                        "/wealth"));
            }
        }
    }

    private String buildSummary(
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net,
            List<FinanceCoachInsightCardResponse> cards) {
        if (income.compareTo(BigDecimal.ZERO) == 0 && expense.compareTo(BigDecimal.ZERO) == 0) {
            return "Mely needs a few transactions before the coach can give precise monthly guidance.";
        }
        String cashFlow = net.compareTo(BigDecimal.ZERO) >= 0 ? "positive" : "negative";
        String focus = cards.isEmpty() ? "Keep tracking your data consistently." : cards.get(0).getTitle();
        return "Your cash flow is " + cashFlow + " this month. Main focus: " + focus + ".";
    }

    private FinanceCoachInsightCardResponse card(
            String type,
            String severity,
            String title,
            String message,
            String actionLabel,
            String actionRoute) {
        return FinanceCoachInsightCardResponse.builder()
                .type(type)
                .severity(severity)
                .title(title)
                .message(message)
                .actionLabel(actionLabel)
                .actionRoute(actionRoute)
                .build();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
