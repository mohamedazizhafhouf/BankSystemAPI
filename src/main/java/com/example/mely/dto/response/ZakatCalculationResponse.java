package com.example.mely.dto.response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZakatCalculationResponse {

     private String userId;
    private LocalDate calculationDate;
    private LocalDate hawlStartDate;
    private LocalDate hawlEndDate;
    private BigDecimal totalAssets;
    private BigDecimal nisabThreshold;
    private String nisabCurrency;
    private BigDecimal nisabInSAR;
    private Boolean isAboveNisab;
    private BigDecimal zakatDueAmount;
    private BigDecimal zakatRate;
    private String zakatStatus;
    private Map<String, BigDecimal> assetBreakdown;
    private String message;

}
