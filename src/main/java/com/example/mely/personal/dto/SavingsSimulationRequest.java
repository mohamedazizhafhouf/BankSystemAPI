package com.example.mely.personal.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SavingsSimulationRequest {

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal initialBalance;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal monthlyContribution;

    @NotNull
    @Min(1)
    @Max(600)
    private Integer months;

    /**
     * Expected annual profit rate in percent (Islamic finance planning — not guaranteed returns).
     */
    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal annualProfitRatePercent;
}
