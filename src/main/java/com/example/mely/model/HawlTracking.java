package com.example.mely.model;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "hawl_tracking")
public class HawlTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    @Column(name = "hawl_start_date")
    private LocalDate hawlStartDate;
    
    @Column(name = "hawl_end_date")
    private LocalDate hawlEndDate;
    
    @Column(name = "last_calculation_date")
    private LocalDate lastCalculationDate;
    
    @Column(name = "next_calculation_date")
    private LocalDate nextCalculationDate;
    
    @Column(name = "is_hawl_completed")
    private Boolean isHawlCompleted;
    
    @Column(name = "days_remaining")
    private Integer daysRemaining;

    @Column(name = "previous_year_zakat_paid")
    private Boolean previousYearZakatPaid;
    
    @Column(name = "previous_payment_date")
    private LocalDate previousPaymentDate;

}
