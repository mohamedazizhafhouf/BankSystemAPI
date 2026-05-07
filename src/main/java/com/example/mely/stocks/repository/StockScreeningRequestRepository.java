package com.example.mely.stocks.repository;

import com.example.mely.stocks.model.StockScreeningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockScreeningRequestRepository extends JpaRepository<StockScreeningRequest, UUID> {
    Optional<StockScreeningRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<StockScreeningRequest> findByIdAndCreatedByUserId(UUID id, UUID createdByUserId);

    List<StockScreeningRequest> findByCreatedByUserIdOrderByCreatedAtDesc(UUID createdByUserId);

    List<StockScreeningRequest> findByCreatedByUserIdOrderByCreatedAtDesc(UUID createdByUserId, Pageable pageable);
}
