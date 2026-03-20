package com.example.mely.service;

import com.example.mely.dto.request.RegistrationRequestDTO;
import com.example.mely.dto.request.VerificationRequestDTO;
import com.example.mely.model.Enums.KycStatus;
import com.example.mely.model.Enums.RoleType;
import com.example.mely.model.Enums.VerificationStatus;
import com.example.mely.model.User;
import com.example.mely.model.UserVerification;
import com.example.mely.repository.UserRepository;
import com.example.mely.repository.UserVerificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final UserVerificationRepository userVerificationRepository;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Phase 1: Create/update staging record and trigger email otp sending.
     * Called when the user submits the first registration form.
     */

    @Transactional
    public void startRegistration(RegistrationRequestDTO dto) {
        // 1. Safety check: if the email already in the main user table
        if (userRepository.existsByEmail(dto.getEmail())){
            throw new RuntimeException("Email is already registered.");
        }

        //2. Generate a secure 6 digit otp
        String otp = otpService.generateOtp(dto.getEmail());

        /**
         * Handling the user verification step.
         * Create or update, depending on the user action
         */
        UserVerification verification = userVerificationRepository.findByEmail(dto.getEmail()).orElse(new UserVerification());

        verification.setFirstName(dto.getFirstName());
        verification.setLastName(dto.getLastName());
        verification.setEmail(dto.getEmail());
        verification.setPhone(dto.getPhone());
        verification.setOtpCode(otp);
        verification.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        verification.setVerificationStatus(VerificationStatus.PENDING);

        userVerificationRepository.save(verification);

        //Send the email with the generated otp
        emailService.sendOtpEmail(dto.getEmail(), otp);
    }

    /**
     * Phase 2: Verify code via OtpService and migrate to permanent User table.
     */

    @Transactional
    public User verifyAndCreateUser(VerificationRequestDTO dto) {
        UserVerification verification = userVerificationRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new RuntimeException("Registration process not found.")
        );

        //Use otpService to validate the incoming code
        boolean isOtpValid = otpService.validateOtp(dto.getEmail(), dto.getOtp());

        if(!isOtpValid){
            throw new RuntimeException("Invalid code.");
        }
        if(verification.getOtpExpiresAt().isBefore(LocalDateTime.now())){
            throw new RuntimeException("Code expired.");
        }

        // Build and save the final User
        User user = User.builder()
                .tenantId(verification.getTenantId())
                .firstName(verification.getFirstName())
                .lastName(verification.getLastName())
                .email(verification.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .roleType(RoleType.USER)
                .kycStatus(KycStatus.PENDING)
                .verificationId(verification.getId())
                .build();

        User savedUser = userRepository.save(user);

        //update verification record to APPROVED
        verification.setVerificationStatus(VerificationStatus.APPROVED);
        userVerificationRepository.save(verification);
        return savedUser;
    }
}
