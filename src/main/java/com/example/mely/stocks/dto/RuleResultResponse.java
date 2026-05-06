package com.example.mely.stocks.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResultResponse {
    private String ruleCode;
    private String label;
    private String status;
    private Boolean passed;
    private String message;
    private Map<String, Object> details;
}
