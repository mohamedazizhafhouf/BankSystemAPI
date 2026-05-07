package com.example.mely.personal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SavingsGoalRequest {

    @NotBlank
    private String title;

    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal targetAmount;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal currentAmount;

    private LocalDate targetDate;
}
