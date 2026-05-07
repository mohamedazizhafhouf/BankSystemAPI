package com.example.mely.metals;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mely.goldapi")
public class GoldApiProperties {

    /**
     * e.g. {@code https://api.gold-api.com} — no trailing slash.
     */
    private String baseUrl = "https://api.gold-api.com";

    /**
     * API key for gold-api.com price history (sent as {@code x-api-key}). Free tier: 10 calls/hour.
     * Spot prices do not require a key. Set env {@code GOLD_API_KEY}; never commit real keys.
     */
    private String apiKey = "";

    /**
     * Legacy alias for {@link #apiKey} (same header). Prefer {@code mely.goldapi.api-key}.
     */
    private String accessToken = "";

    /**
     * Key used for {@code GET /history/...}, or empty if history is disabled.
     */
    public String resolveHistoryApiKey() {
        String k = apiKey == null ? "" : apiKey.trim();
        if (!k.isEmpty()) {
            return k;
        }
        return accessToken == null ? "" : accessToken.trim();
    }

    /**
     * Upper bound for history chart length (each day = 2 HTTP calls: gold + silver).
     */
    private int maxHistoryDays = 14;

    /**
     * Pause between upstream calls to reduce rate-limit issues (milliseconds).
     */
    private long requestDelayMs = 120;
}
