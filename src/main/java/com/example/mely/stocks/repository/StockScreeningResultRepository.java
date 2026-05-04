package com.example.mely.stocks.repository;

import com.example.mely.stocks.model.StockScreeningResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockScreeningResultRepository extends JpaRepository<StockScreeningResult, UUID> {
    Optional<StockScreeningResult> findByRequest_Id(UUID requestId);
}
