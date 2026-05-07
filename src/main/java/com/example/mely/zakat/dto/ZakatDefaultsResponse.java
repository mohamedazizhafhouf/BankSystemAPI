package com.example.mely.zakat.dto;

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
public class ZakatDefaultsResponse {
    private BigDecimal goldNisabGrams;
    private BigDecimal silverNisabGrams;
    private BigDecimal defaultGoldPricePerGram;
    private BigDecimal defaultSilverPricePerGram;
    private BigDecimal zakatRate;
}
