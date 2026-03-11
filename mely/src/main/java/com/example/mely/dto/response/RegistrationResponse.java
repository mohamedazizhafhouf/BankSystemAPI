package com.example.mely.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {
    private String message;
    private UUID userId;
    private String email;
    private String sessionId;
    private Integer currentStep;
    private Boolean completed;
}