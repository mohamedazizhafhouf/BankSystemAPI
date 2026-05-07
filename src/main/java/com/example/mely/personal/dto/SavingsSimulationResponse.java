package com.example.mely.personal.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SavingsSimulationResponse {
    String disclaimer;
    BigDecimal finalBalance;
    List<SavingsSimulationPoint> points;
}
