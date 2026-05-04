package com.example.mely.stocks.service.impl;

import com.example.mely.stocks.model.RuleProfile;
import com.example.mely.stocks.model.StockScreeningRequest;
import com.example.mely.stocks.service.BusinessActivityAnalyzer;
import com.example.mely.stocks.service.analysis.ActivityAnalysisResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class KeywordBusinessActivityAnalyzer implements BusinessActivityAnalyzer {

    @Override
    public ActivityAnalysisResult analyze(StockScreeningRequest request, RuleProfile ruleProfile) {
        String searchableText = Stream.of(request.getSector(), request.getBusinessDescription())
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .reduce("", (left, right) -> left + " " + right)
                .trim();

        List<String> prohibitedKeywords = ruleProfile.getProhibitedKeywords();
        if (prohibitedKeywords != null) {
            for (String keyword : prohibitedKeywords) {
                if (searchableText.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return ActivityAnalysisResult.builder()
                            .compliant(false)
                            .matchedKeyword(keyword)
                            .rationale("The company activity matched prohibited keyword '" + keyword + "'.")
                            .build();
                }
            }
        }

        return ActivityAnalysisResult.builder()
                .compliant(true)
                .rationale("No prohibited business activity keywords were detected in the submitted company profile.")
                .build();
    }
}
