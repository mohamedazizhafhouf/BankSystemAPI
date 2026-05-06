package com.example.mely.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.mely.model.HawlTracking;

@Repository
public interface HawlTrackingRepository extends JpaRepository<HawlTracking, UUID> {

    
    
    Optional<HawlTracking> findByUserId(UUID userId);
    
    List<HawlTracking> findByIsHawlCompletedFalseAndHawlEndDateBefore(LocalDate date);
    
    @Modifying
    @Query("UPDATE HawlTracking h SET h.daysRemaining = ?2 WHERE h.userId = ?1")
    void updateDaysRemaining(UUID userId, Integer daysRemaining);

}
