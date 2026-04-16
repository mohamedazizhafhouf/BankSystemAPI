package com.example.mely.dto.request;

import lombok.Data;

@Data
public class ProvisionBankRequest {
    private String firstName;
    private String lastName;
    private String roleType;
    private String bankName;
    private String adminEmail;
    private String tempPassword;
}
