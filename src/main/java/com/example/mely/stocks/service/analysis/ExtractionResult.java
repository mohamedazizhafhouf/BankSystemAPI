package com.example.mely.stocks.service.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {
    private Map<String, ExtractedMetric> metrics;
    private List<String> warnings;
}
