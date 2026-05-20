package com.example.mely.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FinanceCoachChatRequest {

    @NotBlank
    private String message;

    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must use YYYY-MM format")
    private String yearMonth;
}
