package com.example.mely.service;

import com.example.mely.dto.request.RegisterRequest;
import com.example.mely.dto.response.LoginResponse;
import com.example.mely.dto.response.RegistrationResponse;
import com.example.mely.model.Enums.RoleType;
import com.example.mely.model.Membership;
import com.example.mely.model.User;
import com.example.mely.repository.MembershipRepository;
import com.example.mely.repository.UserRepository;
import com.example.mely.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepo;
    @Autowired private MembershipRepository membershipRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    public RegistrationResponse registerSaasUser(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepo.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .roleType(RoleType.SAAS_USER)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepo.save(user);

        return RegistrationResponse.builder()
                .message("Registration successful")
                .userId(user.getId())
                .email(user.getEmail())
                .sessionId(null)
                .build();
    }

    public LoginResponse authenticateUser(String email, String password) {
        User user = userRepo.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        Membership membership = membershipRepo.findByUser(user).orElse(null);
        if (membership != null) {
            String token = jwtUtils.generateJwtToken(
                    user.getId(),
                    user.getEmail(),
                    membership.getRole().name(),
                    membership.getOrganizationId(),
                    "BANK"
            );

            return LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .email(user.getEmail())
                    .role(membership.getRole().name())
                    .organizationId(membership.getOrganizationId())
                    .accountType("BANK")
                    .message("Login successful")
                    .build();
        }

        if (user.getRoleType() != RoleType.SAAS_USER) {
            throw new RuntimeException("User has no organization assigned");
        }

        String token = jwtUtils.generateJwtToken(
                user.getId(),
                user.getEmail(),
                user.getRoleType().name(),
                null,
                "SAAS"
        );

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .email(user.getEmail())
                .role(user.getRoleType().name())
                .organizationId(null)
                .accountType("SAAS")
                .message("Login successful")
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
