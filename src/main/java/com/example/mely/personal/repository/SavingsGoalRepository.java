package com.example.mely.personal.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.mely.personal.model.SavingsGoal;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID> {

    List<SavingsGoal> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
