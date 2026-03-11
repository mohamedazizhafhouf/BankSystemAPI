package com.example.mely.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KycRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String nationalIdNumber;

    @NotNull
    private LocalDate dateOfBirth;

    private String sessionId; // To maintain registration session
}