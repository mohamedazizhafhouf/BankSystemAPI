package com.example.mely.currency.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CurrencyConvertResponse {
    String from;
    String to;
    BigDecimal amount;
    BigDecimal result;
    BigDecimal rate;
    /** Unix seconds from provider when present. */
    Long providerTimestamp;
    String disclaimer;
}
