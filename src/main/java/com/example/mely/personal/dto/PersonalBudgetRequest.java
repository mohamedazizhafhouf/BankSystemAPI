package com.example.mely.personal.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PersonalBudgetRequest {

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "yearMonth must be YYYY-MM")
    private String yearMonth;

    @NotBlank
    private String categoryCode;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal capAmount;
}
