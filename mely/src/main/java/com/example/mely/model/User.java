package com.example.mely.model;

import com.example.mely.model.Enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type")
    private RoleType roleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    private KycStatus kycStatus;

    @Column(name = "zakat_handling")
    private Boolean zakatHandling;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "verification_id")
    private UUID verificationId;

    @Column(name = "last_login_ip")
    private String lastLoginIp;

    @OneToOne
    @JoinColumn(name = "verification_id", referencedColumnName = "verification_id", insertable = false, updatable = false)
    private UserVerification userVerification;

    @OneToOne(mappedBy = "user")
    private Account account;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (roleType == null) {
            roleType = RoleType.USER;
        }
        if (kycStatus == null) {
            kycStatus = KycStatus.NOT_STARTED;
        }
        if (zakatHandling == null) {
            zakatHandling = false;
        }
    }
}