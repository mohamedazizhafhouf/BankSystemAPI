package com.example.mely.stocks.repository;

import com.example.mely.stocks.model.StockScreeningRequest;
import com.example.mely.stocks.model.StockScreeningStatus;
import com.example.mely.stocks.model.StockVerdict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StockScreeningRequestRepository extends JpaRepository<StockScreeningRequest, UUID> {
    Optional<StockScreeningRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<StockScreeningRequest> findByIdAndCreatedByUserId(UUID id, UUID createdByUserId);

    @Query("""
            select request
            from StockScreeningRequest request
            left join request.result result
            where request.createdByUserId = :createdByUserId
              and (:status is null or request.status = :status)
              and (:verdict is null or result.verdict = :verdict)
              and (:query is null
                   or lower(request.companyName) like lower(concat('%', :query, '%'))
                   or lower(coalesce(request.ticker, '')) like lower(concat('%', :query, '%')))
            """)
    Page<StockScreeningRequest> findHistoryByCreatedByUserId(@Param("createdByUserId") UUID createdByUserId,
                                                             @Param("status") StockScreeningStatus status,
                                                             @Param("verdict") StockVerdict verdict,
                                                             @Param("query") String query,
                                                             Pageable pageable);
}
