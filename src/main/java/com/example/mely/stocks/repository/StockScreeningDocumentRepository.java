package com.example.mely.stocks.repository;

import com.example.mely.stocks.model.StockScreeningDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockScreeningDocumentRepository extends JpaRepository<StockScreeningDocument, UUID> {
    List<StockScreeningDocument> findByRequest_Id(UUID requestId);
}
