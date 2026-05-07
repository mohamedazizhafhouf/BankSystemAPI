package com.example.mely.metals.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetalSpotQuoteResponse {
    private String metal;
    private String currency;
    private BigDecimal pricePerTroyOunce;
    /** Fine metal per gram (spot per troy oz divided by grams per troy ounce). */
    private BigDecimal priceGram24k;
    /** Fine metal per kilogram ({@code priceGram24k} times 1000). */
    private BigDecimal pricePerKilogram;
    private Long timestamp;
    private String error;
}
