package com.example.mely.currency;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * APILayer <a href="https://docs.apilayer.com/currencylayer/docs/quickstart-guide">Currencylayer</a> (Currency Data API).
 * Authenticate with the {@code apikey} header; never expose the key to the browser — proxy via this service only.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mely.currencylayer")
public class CurrencylayerProperties {

    /**
     * e.g. {@code https://api.apilayer.com/currency_data} — no trailing slash.
     */
    private String baseUrl = "https://api.apilayer.com/currency_data";

    /**
     * APILayer API key (sent as header {@code apikey}). Set {@code CURRENCYLAYER_API_KEY} or
     * {@code mely.currencylayer.api-key} in application-local.properties.
     */
    private String apiKey = "";

    public String resolveApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    public boolean hasApiKey() {
        return !resolveApiKey().isEmpty();
    }
}
