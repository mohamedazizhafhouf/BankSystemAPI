package com.example.mely.personal.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.example.mely.model.HawlTracking;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HawlStatusResponse {
    UUID id;
    LocalDate hawlStartDate;
    LocalDate hawlEndDate;
    LocalDate lastCalculationDate;
    Boolean hawlCompleted;
    Integer daysRemaining;

    public static HawlStatusResponse from(HawlTracking h) {
        return HawlStatusResponse.builder()
                .id(h.getId())
                .hawlStartDate(h.getHawlStartDate())
                .hawlEndDate(h.getHawlEndDate())
                .lastCalculationDate(h.getLastCalculationDate())
                .hawlCompleted(h.getIsHawlCompleted())
                .daysRemaining(h.getDaysRemaining())
                .build();
    }
}
