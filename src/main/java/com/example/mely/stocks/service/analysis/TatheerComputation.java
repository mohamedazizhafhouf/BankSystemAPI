package com.example.mely.stocks.service.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TatheerComputation {
    private boolean needsTatheer;
    private BigDecimal tatheerAmount;
    private BigDecimal purificationRatio;
    private BigDecimal purificationPerShare;
}
