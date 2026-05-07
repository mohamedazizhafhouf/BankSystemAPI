package com.example.mely.personal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.mely.personal.model.PersonalBudget;

@Repository
public interface PersonalBudgetRepository extends JpaRepository<PersonalBudget, UUID> {

    List<PersonalBudget> findByUserIdAndYearMonth(UUID userId, String yearMonth);

    Optional<PersonalBudget> findByUserIdAndYearMonthAndCategoryCode(UUID userId, String yearMonth, String categoryCode);
}
