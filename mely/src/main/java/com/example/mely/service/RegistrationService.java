package com.example.mely.service;

import com.example.mely.dto.request.*;
import com.example.mely.dto.response.RegistrationResponse;
import com.example.mely.model.*;
import com.example.mely.model.Enums.*;
import com.example.mely.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final UserVerificationRepository verificationRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    // Temporary storage for registration sessions (in production, use Redis)
    private final Map<String, RegistrationSession> registrationSessions = new HashMap<>();

    @Transactional
    public RegistrationResponse step1AccountSetup(AccountSetupRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create registration session
        String sessionId = UUID.randomUUID().toString();
        RegistrationSession session = RegistrationSession.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .currentStep(1)
                .build();

        registrationSessions.put(sessionId, session);

        return RegistrationResponse.builder()
                .message("Account setup completed")
                .email(request.getEmail())
                .sessionId(sessionId)
                .currentStep(1)
                .completed(false)
                .build();
    }

    @Transactional
    public RegistrationResponse step2Kyc(KycRequest request) {
        RegistrationSession session = registrationSessions.get(request.getSessionId());
        if (session == null) {
            throw new RuntimeException("Invalid session");
        }

        session.setFirstName(request.getFirstName());
        session.setLastName(request.getLastName());
        session.setNationalIdNumber(request.getNationalIdNumber());
        session.setDateOfBirth(request.getDateOfBirth());
        session.setCurrentStep(2);

        registrationSessions.put(request.getSessionId(), session);

        return RegistrationResponse.builder()
                .message("KYC information completed")
                .email(session.getEmail())
                .sessionId(request.getSessionId())
                .currentStep(2)
                .completed(false)
                .build();
    }

    @Transactional
    public RegistrationResponse step3ContactPreferences(ContactPreferencesRequest request) {
        RegistrationSession session = registrationSessions.get(request.getSessionId());
        if (session == null) {
            throw new RuntimeException("Invalid session");
        }

        session.setPhoneNumber(request.getPhoneNumber());
        session.setHomeAddress(request.getHomeAddress());
        session.setZakatHandling(request.getZakatHandling() != null ? request.getZakatHandling() : false);
        session.setCurrentStep(3);

        registrationSessions.put(request.getSessionId(), session);

        return RegistrationResponse.builder()
                .message("Contact and preferences completed")
                .email(session.getEmail())
                .sessionId(request.getSessionId())
                .currentStep(3)
                .completed(false)
                .build();
    }

    @Transactional
    public RegistrationResponse completeRegistration(String sessionId) {
        RegistrationSession session = registrationSessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Invalid session");
        }

        // Create User
        User user = User.builder()
                .tenantId(UUID.randomUUID()) // In production, this would come from the tenant context
                .firstName(session.getFirstName())
                .lastName(session.getLastName())
                .email(session.getEmail())
                .password(session.getPassword())
                .roleType(RoleType.USER)
                .kycStatus(KycStatus.COMPLETED)
                .zakatHandling(session.getZakatHandling())
                .createdAt(LocalDate.now().atStartOfDay())
                .build();

        user = userRepository.save(user);

        // Create Verification record
        UserVerification verification = UserVerification.builder()
                .tenantId(user.getTenantId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(session.getPhoneNumber())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        verification = verificationRepository.save(verification);

        // Update user with verification ID
        user.setVerificationId(verification.getId());
        userRepository.save(user);

        // Create Account
        Account account = Account.builder()
                .tenantId(user.getTenantId())
                .userId(user.getId())
                .accountNumber(generateAccountNumber())
                .contractType(ContractType.SAVINGS)
                .currencyCode("SAR")
                .balance(BigDecimal.ZERO)
                .profitShareRatio(new BigDecimal("0.00"))
                .status(AccountStatus.ACTIVE)
                .openingDate(LocalDate.now())
                .build();

        accountRepository.save(account);

        // Clear session
        registrationSessions.remove(sessionId);

        return RegistrationResponse.builder()
                .message("Registration completed successfully")
                .userId(user.getId())
                .email(user.getEmail())
                .currentStep(3)
                .completed(true)
                .build();
    }

    private String generateAccountNumber() {
        // Generate a unique account number
        String accountNumber;
        do {
            accountNumber = String.format("%016d",
                    (long) (Math.random() * 10000000000000000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    // Inner class for registration session
    @lombok.Data
    @lombok.Builder
    private static class RegistrationSession {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String nationalIdNumber;
        private LocalDate dateOfBirth;
        private String phoneNumber;
        private String homeAddress;
        private Boolean zakatHandling;
        private int currentStep;
    }
}