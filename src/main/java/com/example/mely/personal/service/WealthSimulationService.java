package com.example.mely.personal.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.mely.personal.dto.SavingsSimulationPoint;
import com.example.mely.personal.dto.SavingsSimulationRequest;
import com.example.mely.personal.dto.SavingsSimulationResponse;

@Service
public class WealthSimulationService {

    private static final String DISCLAIMER =
            "Educational projection only. Islamic finance outcomes depend on contracts and profit "
                    + "realization; this is not investment advice or a promise of returns.";

    public SavingsSimulationResponse simulate(SavingsSimulationRequest req) {
        BigDecimal monthlyRate = req.getAnnualProfitRatePercent()
                .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);

        BigDecimal balance = req.getInitialBalance().setScale(4, RoundingMode.HALF_UP);
        List<SavingsSimulationPoint> points = new ArrayList<>();

        points.add(SavingsSimulationPoint.builder().monthIndex(0).balance(balance).build());

        for (int m = 1; m <= req.getMonths(); m++) {
            balance = balance.add(req.getMonthlyContribution());
            balance = balance.multiply(BigDecimal.ONE.add(monthlyRate)).setScale(4, RoundingMode.HALF_UP);
            points.add(SavingsSimulationPoint.builder().monthIndex(m).balance(balance).build());
        }

        return SavingsSimulationResponse.builder()
                .disclaimer(DISCLAIMER)
                .finalBalance(balance)
                .points(points)
                .build();
    }
}
