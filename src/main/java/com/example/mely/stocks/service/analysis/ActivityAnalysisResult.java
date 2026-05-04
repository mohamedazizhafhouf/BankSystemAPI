package com.example.mely.stocks.service.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAnalysisResult {
    private boolean compliant;
    private String matchedKeyword;
    private String rationale;
}
