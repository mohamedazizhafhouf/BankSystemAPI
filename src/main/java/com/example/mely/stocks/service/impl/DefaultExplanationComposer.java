package com.example.mely.stocks.service.impl;

import com.example.mely.stocks.model.StockScreeningRequest;
import com.example.mely.stocks.model.StockVerdict;
import com.example.mely.stocks.service.ExplanationComposer;
import com.example.mely.stocks.service.analysis.ActivityAnalysisResult;
import com.example.mely.stocks.service.analysis.ScreeningComputation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DefaultExplanationComposer implements ExplanationComposer {

    @Override
    public String composeUserMessage(StockScreeningRequest request,
                                     ScreeningComputation computation,
                                     ActivityAnalysisResult activityAnalysisResult) {
        String verdictLine = switch (computation.getVerdict()) {
            case HALAL -> request.getCompanyName() + " passed the current Sharia screening profile.";
            case NON_HALAL -> request.getCompanyName() + " did not pass the current Sharia screening profile.";
            case REVIEW_REQUIRED -> request.getCompanyName() + " needs a manual review because some required financial inputs are still missing.";
        };

        return verdictLine + " " + activityAnalysisResult.getRationale();
    }

    @Override
    public String composeDetailedExplanation(StockScreeningRequest request,
                                             ScreeningComputation computation,
                                             ActivityAnalysisResult activityAnalysisResult) {
        String rules = computation.getRuleResults()
                .stream()
                .map(this::renderRule)
                .collect(Collectors.joining("\n"));

        return """
                Company: %s
                Verdict: %s
                Activity analysis: %s

                Rule trace:
                %s
                """.formatted(
                request.getCompanyName(),
                prettyVerdict(computation.getVerdict()),
                activityAnalysisResult.getRationale(),
                rules
        ).trim();
    }

    private String renderRule(Map<String, Object> rule) {
        String label = String.valueOf(rule.get("label"));
        String status = String.valueOf(rule.get("status"));
        String message = String.valueOf(rule.get("message"));
        Object details = rule.get("details");
        return "- " + label + " [" + status + "]: " + message + renderDetails(details);
    }

    private String renderDetails(Object details) {
        if (!(details instanceof Map<?, ?> detailMap) || detailMap.isEmpty()) {
            return "";
        }

        List<String> fragments = detailMap.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
        return " (" + String.join(", ", fragments) + ")";
    }

    private String prettyVerdict(StockVerdict verdict) {
        return switch (verdict) {
            case HALAL -> "Halal";
            case NON_HALAL -> "Non-halal";
            case REVIEW_REQUIRED -> "Review required";
        };
    }
}
