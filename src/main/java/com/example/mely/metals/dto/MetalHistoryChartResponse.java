package com.example.mely.metals.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetalHistoryChartResponse {
    private List<String> labels;
    private List<BigDecimal> goldPricePerGram24k;
    private List<BigDecimal> silverPricePerGram24k;
    private String currency;
    /** Optional message when history is unavailable or degraded (e.g. missing access token). */
    private String note;
}
