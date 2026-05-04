package com.example.mely.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.mely.model.Enums.NisabType;

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
@Table(name = "nisab_configurations")

public class NisabConfiguration {

@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "nisab_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NisabType nisabType;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal value;
    
    @Column(nullable = false, length = 3)
    private String currencyCode;
    
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;
    
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    private String source; // e.g., "Gold Price", "Silver Price", "Manual"
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

     @Column(name = "updated_by")
    private String updatedBy;

}
