package com.example.mely.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType;
    private String email;
    private String role;
    private UUID organizationId;
    private String accountType;
    private String message;
}
