package com.example.mely.controller;


import com.example.mely.dto.request.RegistrationRequestDTO;
import com.example.mely.dto.request.VerificationRequestDTO;
import com.example.mely.model.User;
import com.example.mely.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegistrationService registrationService;

    /**
     * POST /api/auth/register
     * Step 1: Collect info, generate OTP, send email.
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegistrationRequestDTO request) {
        registrationService.startRegistration(request);
        return ResponseEntity.ok("OTP sent successfully to " + request.getEmail());
    }

    /**
     * POST /api/auth/verify-otp
     * Step 2: Validate OTP and Password, then create the official User.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<User> verifyOtp(@RequestBody VerificationRequestDTO request) {
        User createdUser = registrationService.verifyAndCreateUser(request);
        return ResponseEntity.ok(createdUser);
    }
}

