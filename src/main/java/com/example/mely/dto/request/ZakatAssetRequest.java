package com.example.mely.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.mely.model.Enums.AssetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ZakatAssetRequest {

    private UUID userId;
    private AssetType assetType;
    private BigDecimal amount;
    private BigDecimal value;
    private String currencyCode;
    private String description;
    private LocalDateTime acquisitionDate;
    private BigDecimal weight; // For gold/silver
    private BigDecimal purity; // For gold/silver
    private BigDecimal annualReturnRate; // For investments

}
