package com.example.mely.stocks.dto;

import com.example.mely.stocks.model.DocumentExtractionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDocumentResponse {
    private UUID id;
    private String originalFileName;
    private String contentType;
    private long size;
    private DocumentExtractionStatus extractionStatus;
}
