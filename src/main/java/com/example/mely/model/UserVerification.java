package com.example.mely.model;

import com.example.mely.model.Enums.DocumentType;
import com.example.mely.model.Enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_verification")
public class UserVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String documentUrl;

    private String otpCode;

    private LocalDateTime otpExpiresAt;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    private String rejectionReason;

    @Column(name = "assigned_agent")
    private UUID assignedAgent;
}