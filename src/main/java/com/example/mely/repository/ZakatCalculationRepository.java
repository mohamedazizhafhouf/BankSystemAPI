package com.example.mely.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.mely.model.Enums.ZakatStatus;
import com.example.mely.model.ZakatCalculation;


public interface ZakatCalculationRepository extends JpaRepository<ZakatCalculation, UUID> { List<ZakatCalculation> findByUserIdOrderByCalculationDateDesc(UUID userId);
    
    Optional<ZakatCalculation> findTopByUserIdOrderByCalculationDateDesc(UUID userId);
    
    List<ZakatCalculation> findByUserIdAndZakatStatus(UUID userId, ZakatStatus status);
    
    List<ZakatCalculation> findByZakatStatusAndCalculationDateBefore(ZakatStatus status, LocalDate date);

}
