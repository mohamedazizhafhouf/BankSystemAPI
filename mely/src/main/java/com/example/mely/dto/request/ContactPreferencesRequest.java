package com.example.mely.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactPreferencesRequest {

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String homeAddress;

    private Boolean zakatHandling;

    private String sessionId;
}