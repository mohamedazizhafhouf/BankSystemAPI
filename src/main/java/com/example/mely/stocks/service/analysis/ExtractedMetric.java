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
public class ExtractedMetric {
    private BigDecimal value;
    private String sourceDocument;
    private Integer pageNumber;
    private String snippet;
}
