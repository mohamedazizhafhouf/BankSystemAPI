package com.example.mely.stocks.repository;

import com.example.mely.stocks.model.RuleProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RuleProfileRepository extends JpaRepository<RuleProfile, UUID> {
    Optional<RuleProfile> findFirstByActiveTrueOrderByCreatedAtAsc();
}
