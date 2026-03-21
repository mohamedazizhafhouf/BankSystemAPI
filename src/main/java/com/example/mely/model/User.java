package com.example.mely.model;

import com.example.mely.model.Enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false,
            columnDefinition = "VARCHAR(20) DEFAULT 'PENDING' CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED'))")
    private KycStatus kycStatus = KycStatus.PENDING;

    private Boolean zakatHandling;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "verification_id")
    private UUID verificationId;

    private String lastLoginIp;
}