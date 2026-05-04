package com.example.mely.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.mely.model.Enums.ZakatStatus;

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
@Table(name = "zakat_calculations")

public class ZakatCalculation {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "calculation_date")
    private LocalDate calculationDate;
    
    @Column(name = "hawl_start_date")
    private LocalDate hawlStartDate;
    
    @Column(name = "hawl_end_date")
    private LocalDate hawlEndDate;
    
    @Column(name = "total_assets", precision = 19, scale = 4)
    private BigDecimal totalAssets;
    
    @Column(name = "nisab_threshold", precision = 19, scale = 4)
    private BigDecimal nisabThreshold;
    
    @Column(name = "nisab_currency")
    private String nisabCurrency;
    
    @Column(name = "zakat_due_amount", precision = 19, scale = 4)
    private BigDecimal zakatDueAmount;

     @Column(name = "zakat_rate")
    private BigDecimal zakatRate; // 2.5% typically
    
    @Column(name = "zakat_status")
    @Enumerated(EnumType.STRING)
    private ZakatStatus zakatStatus;
    
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    
    @Column(name = "payment_reference")
    private String paymentReference;
    
    private String notes;
    
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
    
    // Asset breakdown stored as JSON
    @Column(columnDefinition = "jsonb")
    private String assetBreakdown;


}
