package com.example.mely.personal.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.mely.personal.model.FinanceCoachInsight;

@Repository
public interface FinanceCoachInsightRepository extends JpaRepository<FinanceCoachInsight, UUID> {

    Optional<FinanceCoachInsight> findByUserIdAndYearMonth(UUID userId, String yearMonth);
}
