package com.example.mely.personal.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SavingsSimulationPoint {
    int monthIndex;
    BigDecimal balance;
}
