package com.example.mely.personal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.mely.personal.model.PersonalTransactionDirection;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PersonalTransactionRequest {

    @NotNull
    private LocalDate occurredOn;

    @NotNull
    private PersonalTransactionDirection direction;

    @NotBlank
    private String categoryCode;

    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal amount;

    private String memo;
}
