package com.example.mely.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegistrationStep3Request {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    private String homeAddress;

    private Boolean zakatHandling;
}