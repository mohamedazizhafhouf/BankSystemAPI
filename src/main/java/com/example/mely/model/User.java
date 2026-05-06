package com.example.mely.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.example.mely.model.Enums.RoleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "bank_id")
    private UUID bankId;

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

    @Column(nullable = false)
    private boolean mustChangePassword = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private String lastLoginIp;


    @Column(name = "zakat_handling")
    private Boolean zakatHandling;
    
    
    
    @Column(name = "verification_id")
    private UUID verificationId;
    
    
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "home_address")
    private String homeAddress;
    
    @Column(name = "national_id_number")
    private String nationalIdNumber;
    
    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;    

    public void updateZakatHandling(Boolean enabled, com.example.mely.service.ZakatService zakatService) {
        this.zakatHandling = enabled;
        // If enabling, initialize hawl tracking
        if (enabled && zakatService != null && this.id != null) {
            zakatService.initializeHawlTracking(this.id);
        }
    }

     public void updateZakatHandling(Boolean enabled) {
        this.zakatHandling = enabled;
    }

    public boolean isZakatEnabled() {
        return Boolean.TRUE.equals(this.zakatHandling);
    }

     public String getFullName() {
        return this.firstName + " " + this.lastName;
    }


    public void updateLastLoginIp(String ipAddress) {
        this.lastLoginIp = ipAddress;
    }
        
}