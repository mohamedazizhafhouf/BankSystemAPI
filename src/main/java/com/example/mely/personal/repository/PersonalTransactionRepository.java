package com.example.mely.personal.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.mely.personal.model.PersonalTransaction;

@Repository
public interface PersonalTransactionRepository extends JpaRepository<PersonalTransaction, UUID> {

    List<PersonalTransaction> findByUserIdAndOccurredOnBetweenOrderByOccurredOnDesc(
            UUID userId, LocalDate from, LocalDate to);

    @Query("""
            select t.categoryCode, t.direction, coalesce(sum(t.amount), 0)
            from PersonalTransaction t
            where t.userId = :userId and t.occurredOn between :from and :to
            group by t.categoryCode, t.direction
            """)
    List<Object[]> sumByCategoryAndDirection(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
