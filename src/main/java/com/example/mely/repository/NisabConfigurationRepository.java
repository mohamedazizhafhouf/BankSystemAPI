package com.example.mely.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.mely.model.Enums.NisabType;
import com.example.mely.model.NisabConfiguration;

@Repository
public interface NisabConfigurationRepository extends JpaRepository<NisabConfiguration, UUID> {
    Optional<NisabConfiguration> findByIsActiveTrueAndNisabType(NisabType nisabType);
    
    @Query("SELECT n FROM NisabConfiguration n WHERE n.isActive = true AND n.effectiveFrom <= :date AND (n.effectiveTo IS NULL OR n.effectiveTo >= :date)")
    Optional<NisabConfiguration> findActiveConfigurationAtDate(LocalDateTime date);

}
