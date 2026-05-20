package com.example.mely.personal.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.mely.personal.model.FinanceCoachMessage;

@Repository
public interface FinanceCoachMessageRepository extends JpaRepository<FinanceCoachMessage, UUID> {

    List<FinanceCoachMessage> findByUserIdOrderByCreatedAtAsc(UUID userId);

    List<FinanceCoachMessage> findByUserIdAndYearMonthOrderByCreatedAtAsc(
            UUID userId, String yearMonth, Pageable pageable);
}
