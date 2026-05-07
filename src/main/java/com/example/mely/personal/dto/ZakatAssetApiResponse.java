package com.example.mely.personal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.mely.model.Enums.AssetType;
import com.example.mely.model.ZakatAsset;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ZakatAssetApiResponse {
    UUID id;
    AssetType assetType;
    BigDecimal amount;
    BigDecimal value;
    String currencyCode;
    String description;
    LocalDateTime acquisitionDate;
    LocalDateTime lastUpdated;
    Boolean active;
    BigDecimal weight;
    BigDecimal purity;
    BigDecimal annualReturnRate;

    public static ZakatAssetApiResponse from(ZakatAsset a) {
        return ZakatAssetApiResponse.builder()
                .id(a.getId())
                .assetType(a.getAssetType())
                .amount(a.getAmount())
                .value(a.getValue())
                .currencyCode(a.getCurrencyCode())
                .description(a.getDescription())
                .acquisitionDate(a.getAcquisitionDate())
                .lastUpdated(a.getLastUpdated())
                .active(a.getIsActive())
                .weight(a.getWeight())
                .purity(a.getPurity())
                .annualReturnRate(a.getAnnualReturnRate())
                .build();
    }
}
