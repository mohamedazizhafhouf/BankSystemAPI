package com.example.mely.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.mely.model.Enums.AssetType;

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
@Table(name = "zakat_assets")
public class ZakatAsset {

 @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "asset_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AssetType assetType;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal value;
    
    @Column(length = 3)
    private String currencyCode;
    
    private String description;

       @Column(name = "acquisition_date")
    private LocalDateTime acquisitionDate;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    // For gold/silver - weight in grams
    private BigDecimal weight;
    
    // For gold/silver - purity percentage
    private BigDecimal purity;
    
    // For investments - annual return rate
    @Column(name = "annual_return_rate")
    private BigDecimal annualReturnRate;



}
