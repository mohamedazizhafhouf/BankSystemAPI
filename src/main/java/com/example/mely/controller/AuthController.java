package com.example.mely.controller;


import com.example.mely.dto.request.RegistrationRequestDTO;
import com.example.mely.dto.request.VerificationRequestDTO;
import com.example.mely.dto.response.AuthResponse;
import com.example.mely.dto.response.UserResponse;
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
    public ResponseEntity<AuthResponse> register(@RequestBody RegistrationRequestDTO request) {
        registrationService.startRegistration(request);
        AuthResponse response = new AuthResponse().builder()
                .message("OTP sent successfully to " + request.getEmail())
                .success(true)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/verify-otp
     * Step 2: Validate OTP and Password, then create the official User.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<UserResponse> verifyOtp(@RequestBody VerificationRequestDTO request) {
        User createdUser = registrationService.verifyAndCreateUser(request);
        UserResponse response = new UserResponse(
                createdUser.getId(),
                createdUser.getFirstName(),
                createdUser.getLastName(),
                createdUser.getEmail(),
                createdUser.getKycStatus(),
                createdUser.getZakatHandling());
        return ResponseEntity.ok(response);
    }
}

