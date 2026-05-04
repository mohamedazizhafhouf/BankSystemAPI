package com.example.mely.stocks.service.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedMetric {
    private BigDecimal value;
    private String source;
    private String sourceDocument;
    private Integer pageNumber;
    private String snippet;

    public Map<String, Object> toResponseMap() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("value", value);
        response.put("source", source);
        response.put("sourceDocument", sourceDocument);
        response.put("pageNumber", pageNumber);
        response.put("snippet", snippet);
        return response;
    }
}
