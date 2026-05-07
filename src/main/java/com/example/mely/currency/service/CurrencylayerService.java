package com.example.mely.currency.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.mely.currency.CurrencylayerProperties;
import com.example.mely.currency.dto.CurrencyConvertResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencylayerService {

    private static final String DISCLAIMER =
            "Indicative midpoint-style rate from the data provider; not a trading quote or investment advice.";

    private final CurrencylayerProperties properties;
    private final RestClient currencylayerRestClient;
    private final ObjectMapper objectMapper;

    public CurrencyConvertResponse convert(String fromRaw, String toRaw, BigDecimal amount) {
        if (!properties.hasApiKey()) {
            throw new IllegalStateException(
                    "Currency conversion is not configured. Set mely.currencylayer.api-key or CURRENCYLAYER_API_KEY.");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        String from = normalizeCurrencyCode(fromRaw);
        String to = normalizeCurrencyCode(toRaw);
        if (from.equals(to)) {
            return CurrencyConvertResponse.builder()
                    .from(from)
                    .to(to)
                    .amount(amount.setScale(8, RoundingMode.HALF_UP))
                    .result(amount.setScale(8, RoundingMode.HALF_UP))
                    .rate(BigDecimal.ONE)
                    .providerTimestamp(null)
                    .disclaimer(DISCLAIMER)
                    .build();
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }

        String normalizedAmount = amount.stripTrailingZeros().toPlainString();
        String uri = UriComponentsBuilder.fromPath("/convert")
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("amount", normalizedAmount)
                .build(true)
                .toUriString();

        try {
            String body = currencylayerRestClient.get().uri(uri).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);

            if (root.path("success").isBoolean() && !root.path("success").asBoolean()) {
                String err = root.path("error").path("info").asText("Currency provider rejected the request.");
                throw new IllegalArgumentException(err);
            }

            BigDecimal result = readDecimal(root.path("result"));
            JsonNode quoteNode = root.path("info").path("quote");
            BigDecimal rate = quoteNode.isMissingNode() || quoteNode.isNull() ? null : readDecimal(quoteNode);
            long ts = root.path("info").path("timestamp").asLong(0L);

            return CurrencyConvertResponse.builder()
                    .from(from)
                    .to(to)
                    .amount(amount.setScale(8, RoundingMode.HALF_UP))
                    .result(result.setScale(8, RoundingMode.HALF_UP))
                    .rate(rate != null ? rate.setScale(10, RoundingMode.HALF_UP) : null)
                    .providerTimestamp(ts > 0 ? ts : null)
                    .disclaimer(DISCLAIMER)
                    .build();
        } catch (RestClientResponseException ex) {
            // Some plans/key types do not allow /convert. Fallback to /live quotes.
            if (ex.getStatusCode().is4xxClientError()) {
                try {
                    return convertWithLiveFallback(from, to, amount);
                } catch (Exception fallbackEx) {
                    log.warn("Currencylayer fallback /live failed: {}", fallbackEx.getMessage());
                }
                try {
                    return convertWithPublicRatesFallback(from, to, amount);
                } catch (Exception backupEx) {
                    log.warn("Public rates fallback failed: {}", backupEx.getMessage());
                }
            }
            throw mapUpstreamHttpException(ex);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("Currencylayer upstream error: {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Currency provider is temporarily unavailable. Try again later.");
        } catch (Exception ex) {
            log.warn("Currencylayer parse error", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unexpected response from currency provider.");
        }
    }

    private static BigDecimal readDecimal(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return BigDecimal.ZERO;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        return new BigDecimal(node.asText());
    }

    private ResponseStatusException mapUpstreamHttpException(RestClientResponseException ex) {
        String upstreamBody = ex.getResponseBodyAsString();
        String detail = parseUpstreamErrorDetail(upstreamBody);
        int code = ex.getStatusCode().value();
        String base = "Currency provider request failed (HTTP " + code + ").";
        String message = detail.isEmpty() ? base : base + " " + detail;
        log.warn("Currencylayer upstream HTTP {}: {}", code, detail.isEmpty() ? ex.getMessage() : detail);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }

    private String parseUpstreamErrorDetail(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String info = root.path("error").path("info").asText("");
            if (!info.isBlank()) {
                return info;
            }
            String message = root.path("message").asText("");
            if (!message.isBlank()) {
                return message;
            }
            return "";
        } catch (Exception ignore) {
            return "";
        }
    }

    private CurrencyConvertResponse convertWithLiveFallback(String from, String to, BigDecimal amount) {
        String liveUri = UriComponentsBuilder.fromPath("/live")
                .queryParam("source", "USD")
                .queryParam("currencies", from + "," + to)
                .build(true)
                .toUriString();

        try {
            String body = currencylayerRestClient.get().uri(liveUri).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            if (root.path("success").isBoolean() && !root.path("success").asBoolean()) {
                String err = root.path("error").path("info").asText("Currency provider rejected the fallback request.");
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, err);
            }

            JsonNode quotes = root.path("quotes");
            BigDecimal usdFrom = readDecimal(quotes.path("USD" + from));
            BigDecimal usdTo = readDecimal(quotes.path("USD" + to));
            if (usdFrom.compareTo(BigDecimal.ZERO) <= 0 || usdTo.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Currency provider returned incomplete live quotes.");
            }

            BigDecimal rate = usdTo.divide(usdFrom, 10, RoundingMode.HALF_UP);
            BigDecimal result = amount.multiply(rate).setScale(8, RoundingMode.HALF_UP);
            long ts = root.path("timestamp").asLong(0L);

            return CurrencyConvertResponse.builder()
                    .from(from)
                    .to(to)
                    .amount(amount.setScale(8, RoundingMode.HALF_UP))
                    .result(result)
                    .rate(rate)
                    .providerTimestamp(ts > 0 ? ts : null)
                    .disclaimer(DISCLAIMER + " (Computed from live quotes fallback)")
                    .build();
        } catch (RestClientResponseException ex) {
            throw mapUpstreamHttpException(ex);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Fallback conversion failed.");
        }
    }

    private CurrencyConvertResponse convertWithPublicRatesFallback(String from, String to, BigDecimal amount) {
        try {
            String body = currencylayerRestClient.get()
                    .uri("https://open.er-api.com/v6/latest/USD")
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            if (!"success".equalsIgnoreCase(root.path("result").asText(""))) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Public rates fallback was not successful.");
            }

            JsonNode rates = root.path("rates");
            BigDecimal usdFrom = readDecimal(rates.path(from));
            BigDecimal usdTo = readDecimal(rates.path(to));
            if (usdFrom.compareTo(BigDecimal.ZERO) <= 0 || usdTo.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Public rates fallback returned missing currencies.");
            }

            BigDecimal rate = usdTo.divide(usdFrom, 10, RoundingMode.HALF_UP);
            BigDecimal result = amount.multiply(rate).setScale(8, RoundingMode.HALF_UP);
            long ts = root.path("time_last_update_unix").asLong(0L);

            return CurrencyConvertResponse.builder()
                    .from(from)
                    .to(to)
                    .amount(amount.setScale(8, RoundingMode.HALF_UP))
                    .result(result)
                    .rate(rate)
                    .providerTimestamp(ts > 0 ? ts : null)
                    .disclaimer(
                            DISCLAIMER
                                    + " (Currencylayer unavailable, temporarily computed from public backup rates feed)")
                    .build();
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Public rates fallback is unavailable.");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Public rates fallback parsing failed.");
        }
    }

    private static String normalizeCurrencyCode(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("currency code is required");
        }
        String c = raw.trim().toUpperCase();
        if (!c.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency code must be exactly 3 letters (ISO 4217)");
        }
        return c;
    }
}
