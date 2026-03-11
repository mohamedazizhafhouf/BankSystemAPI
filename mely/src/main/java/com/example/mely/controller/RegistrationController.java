package com.example.mely.controller;

import com.example.mely.dto.request.*;
import com.example.mely.dto.response.RegistrationResponse;
import com.example.mely.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/step1")
    public ResponseEntity<RegistrationResponse> step1AccountSetup(
            @Valid @RequestBody AccountSetupRequest request) {
        return ResponseEntity.ok(registrationService.step1AccountSetup(request));
    }

    @PostMapping("/step2")
    public ResponseEntity<RegistrationResponse> step2Kyc(
            @Valid @RequestBody KycRequest request) {
        return ResponseEntity.ok(registrationService.step2Kyc(request));
    }

    @PostMapping("/step3")
    public ResponseEntity<RegistrationResponse> step3ContactPreferences(
            @Valid @RequestBody ContactPreferencesRequest request) {
        return ResponseEntity.ok(registrationService.step3ContactPreferences(request));
    }

    @PostMapping("/complete/{sessionId}")
    public ResponseEntity<RegistrationResponse> completeRegistration(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(registrationService.completeRegistration(sessionId));
    }
}