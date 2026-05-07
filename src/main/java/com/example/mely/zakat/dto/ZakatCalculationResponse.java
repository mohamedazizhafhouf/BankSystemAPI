package com.example.mely.zakat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZakatCalculationResponse {
    private BigDecimal netZakatable;
    private BigDecimal nisabThreshold;
    private boolean meetsNisab;
    private BigDecimal zakatDue;
    private BigDecimal hypotheticalZakat;
    private BigDecimal rate;

    @Builder.Default
    private List<ZakatBreakdownLineResponse> breakdown = new ArrayList<>();

    private String disclaimer;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
