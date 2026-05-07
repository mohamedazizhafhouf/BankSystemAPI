package com.example.mely.metals.service;

import com.example.mely.metals.GoldApiProperties;
import com.example.mely.metals.dto.MetalHistoryChartResponse;
import com.example.mely.metals.dto.MetalSpotQuoteResponse;
import com.example.mely.metals.dto.MetalsSpotBundleResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
public class GoldApiService {

    /** Grams per troy ounce (LBMA / UK definition). */
    private static final BigDecimal GRAMS_PER_TROY_OUNCE = new BigDecimal("31.1034768");

    private final GoldApiProperties properties;
    private final RestClient goldApiRestClient;
    private final ObjectMapper objectMapper;

    public GoldApiService(GoldApiProperties properties, RestClient goldApiRestClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.goldApiRestClient = goldApiRestClient;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank();
    }

    public MetalsSpotBundleResponse fetchSpotBundle(String currency) {
        requireConfigured();
        String ccy = normalizeCurrency(currency);
        return MetalsSpotBundleResponse.builder()
                .currency(ccy)
                .gold(fetchSpot("XAU", ccy))
                .silver(fetchSpot("XAG", ccy))
                .build();
    }

    public MetalHistoryChartResponse fetchHistoryChart(String currency, int requestedDays) {
        requireConfigured();
        String ccy = normalizeCurrency(currency);
        int days = Math.max(1, Math.min(requestedDays, properties.getMaxHistoryDays()));

        String historyKey = properties.resolveHistoryApiKey();
        if (historyKey.isEmpty()) {
            return MetalHistoryChartResponse.builder()
                    .currency(ccy)
                    .labels(List.of())
                    .goldPricePerGram24k(List.of())
                    .silverPricePerGram24k(List.of())
                    .note("Price history requires a gold-api.com API key (x-api-key). Set env GOLD_API_KEY or MELY_GOLD_API_KEY, or mely.goldapi.api-key in application-local.properties with --spring.profiles.active=local (see application-local.properties.example). Legacy: mely.goldapi.access-token.")
                    .build();
        }

        try {
            HistoryParseOutcome goldOut = fetchDailyHistoryGramsPerMetal("XAU", days, historyKey);
            sleepBetweenCalls();
            HistoryParseOutcome silverOut = fetchDailyHistoryGramsPerMetal("XAG", days, historyKey);

            NavigableMap<LocalDate, BigDecimal> gold = goldOut.map();
            NavigableMap<LocalDate, BigDecimal> silver = silverOut.map();

            if (gold.isEmpty() && silver.isEmpty()) {
                return MetalHistoryChartResponse.builder()
                        .currency(ccy)
                        .labels(List.of())
                        .goldPricePerGram24k(List.of())
                        .silverPricePerGram24k(List.of())
                        .note(emptyHistoryNote(goldOut, silverOut))
                        .build();
            }

            TreeSet<LocalDate> dates = new TreeSet<>();
            dates.addAll(gold.keySet());
            dates.addAll(silver.keySet());

            List<String> labels = new ArrayList<>();
            List<BigDecimal> goldSeries = new ArrayList<>();
            List<BigDecimal> silverSeries = new ArrayList<>();
            for (LocalDate d : dates) {
                labels.add(d.toString());
                goldSeries.add(gold.get(d));
                silverSeries.add(silver.get(d));
            }

            return MetalHistoryChartResponse.builder()
                    .currency(ccy)
                    .labels(labels)
                    .goldPricePerGram24k(goldSeries)
                    .silverPricePerGram24k(silverSeries)
                    .build();
        } catch (RestClientException ex) {
            return MetalHistoryChartResponse.builder()
                    .currency(ccy)
                    .labels(List.of())
                    .goldPricePerGram24k(List.of())
                    .silverPricePerGram24k(List.of())
                    .note("Price history request failed: " + ex.getMessage())
                    .build();
        } catch (Exception ex) {
            return MetalHistoryChartResponse.builder()
                    .currency(ccy)
                    .labels(List.of())
                    .goldPricePerGram24k(List.of())
                    .silverPricePerGram24k(List.of())
                    .note("Failed to parse price history: " + ex.getMessage())
                    .build();
        }
    }

    /** gold-api.com expects {@code x-api-key}, not Bearer auth. */
    private static final String X_API_KEY_HEADER = "x-api-key";

    /**
     * gold-api.com: {@code GET /history} with {@code symbol}, {@code startTimestamp}, {@code endTimestamp} (Unix seconds),
     * plus {@code groupBy}, {@code aggregation}, {@code orderBy}.
     */
    private HistoryParseOutcome fetchDailyHistoryGramsPerMetal(String metal, int days, String apiKey) {
        ZoneOffset utc = ZoneOffset.UTC;
        LocalDate today = LocalDate.now(utc);
        LocalDate from = today.minusDays((long) days);
        long endTs = Instant.now().getEpochSecond();
        long startTs = from.atStartOfDay(utc).toEpochSecond();
        final long historyStartTs = startTs >= endTs ? endTs - 86_400L : startTs;
        final long historyEndTs = endTs;

        String body = goldApiRestClient.get()
                .uri(uriBuilder -> historyUri(uriBuilder, metal, historyStartTs, historyEndTs))
                .accept(MediaType.APPLICATION_JSON)
                .header(X_API_KEY_HEADER, apiKey)
                .retrieve()
                .body(String.class);
        return parseHistoryToGramsPerDay(body, metal);
    }

    private record HistoryParseOutcome(NavigableMap<LocalDate, BigDecimal> map, String diagnostics) {
        static HistoryParseOutcome empty(String diagnostics) {
            return new HistoryParseOutcome(new TreeMap<>(), diagnostics);
        }
    }

    private static String emptyHistoryNote(HistoryParseOutcome gold, HistoryParseOutcome silver) {
        String g = gold.diagnostics() == null ? "" : gold.diagnostics().trim();
        String s = silver.diagnostics() == null ? "" : silver.diagnostics().trim();
        if (!g.isEmpty() && !s.isEmpty() && g.equals(s)) {
            return "Price history returned no usable points. " + g;
        }
        StringBuilder sb = new StringBuilder("Price history returned no usable points.");
        if (!g.isEmpty()) {
            sb.append(" Gold: ").append(g);
        }
        if (!s.isEmpty()) {
            sb.append(" Silver: ").append(s);
        }
        return sb.toString();
    }

    private URI historyUri(UriBuilder uriBuilder, String metal, long startTs, long endTs) {
        return uriBuilder.path("/history")
                .queryParam("symbol", metal)
                .queryParam("startTimestamp", startTs)
                .queryParam("endTimestamp", endTs)
                .queryParam("groupBy", "day")
                .queryParam("aggregation", "avg")
                .queryParam("orderBy", "asc")
                .build();
    }

    private MetalSpotQuoteResponse fetchSpot(String metal, String currency) {
        try {
            String path = "/price/%s/%s".formatted(metal, currency);
            String body = goldApiRestClient.get()
                    .uri(path)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            return mapQuote(metal, currency, root);
        } catch (RestClientException ex) {
            return MetalSpotQuoteResponse.builder()
                    .metal(metal)
                    .currency(currency)
                    .error("Upstream request failed: " + ex.getMessage())
                    .build();
        } catch (Exception ex) {
            return MetalSpotQuoteResponse.builder()
                    .metal(metal)
                    .currency(currency)
                    .error("Failed to parse response: " + ex.getMessage())
                    .build();
        }
    }

    private MetalSpotQuoteResponse mapQuote(String metal, String currency, JsonNode root) {
        if (root.hasNonNull("error")) {
            return MetalSpotQuoteResponse.builder()
                    .metal(metal)
                    .currency(currency)
                    .error(root.get("error").asText())
                    .build();
        }

        BigDecimal oz = decimal(root, "price");
        BigDecimal perGram = oz == null ? null : perGramFromTroyOunce(oz);
        BigDecimal perKg = perGram == null ? null : perKilogramFromGram(perGram);

        Long ts = null;
        if (root.hasNonNull("updatedAt")) {
            try {
                ts = Instant.parse(root.get("updatedAt").asText()).getEpochSecond();
            } catch (DateTimeParseException ignored) {
                ts = null;
            }
        }

        String metalOut = root.hasNonNull("symbol") ? root.get("symbol").asText() : metal;
        String ccyOut = root.hasNonNull("currency") ? root.get("currency").asText() : currency;

        return MetalSpotQuoteResponse.builder()
                .metal(metalOut)
                .currency(ccyOut)
                .pricePerTroyOunce(oz)
                .priceGram24k(perGram)
                .pricePerKilogram(perKg)
                .timestamp(ts)
                .build();
    }

    private HistoryParseOutcome parseHistoryToGramsPerDay(String body, String symbolForDiag) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            return HistoryParseOutcome.empty("Response is not valid JSON.");
        }
        if (root.hasNonNull("error")) {
            return HistoryParseOutcome.empty("Upstream error: " + root.get("error").asText());
        }
        NavigableMap<LocalDate, BigDecimal> fromMap = tryParseHistoryAsDatePriceObject(root);
        if (!fromMap.isEmpty()) {
            return new HistoryParseOutcome(fromMap, null);
        }

        JsonNode arr = resolveHistoryArray(root);
        NavigableMap<LocalDate, BigDecimal> map = new TreeMap<>();
        if (arr == null) {
            return HistoryParseOutcome.empty("No JSON array found in history response for " + symbolForDiag + ". Top-level keys: " + topLevelKeysSample(root));
        }
        if (!arr.isArray()) {
            return HistoryParseOutcome.empty("Expected array for " + symbolForDiag + "; got " + arr.getNodeType());
        }
        if (arr.isEmpty()) {
            return HistoryParseOutcome.empty("Upstream returned an empty history array for " + symbolForDiag + ".");
        }
        for (JsonNode node : arr) {
            LocalDate d = parseHistoryDate(node);
            BigDecimal oz = pickPricePerOz(node);
            if (d != null && oz != null) {
                map.put(d, perGramFromTroyOunce(oz));
            }
        }
        if (map.isEmpty()) {
            return HistoryParseOutcome.empty(
                    "Could not parse date/price from history rows for " + symbolForDiag + ". First row field names: "
                            + firstObjectFieldNames(arr.size() > 0 ? arr.get(0) : null));
        }
        return new HistoryParseOutcome(map, null);
    }

    private static String topLevelKeysSample(JsonNode root) {
        if (root == null || !root.isObject()) {
            return "(not an object)";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = root.fieldNames();
        int n = 0;
        while (it.hasNext() && n < 12) {
            if (n > 0) {
                sb.append(", ");
            }
            sb.append(it.next());
            n++;
        }
        if (it.hasNext()) {
            sb.append(", …");
        }
        return sb.toString();
    }

    private static String firstObjectFieldNames(JsonNode node) {
        if (node == null || !node.isObject()) {
            return "(none)";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = node.fieldNames();
        int n = 0;
        while (it.hasNext() && n < 20) {
            if (n > 0) {
                sb.append(", ");
            }
            sb.append(it.next());
            n++;
        }
        if (it.hasNext()) {
            sb.append(", …");
        }
        return sb.toString();
    }

    /**
     * Handles {@code {"2024-05-01": 2300, ...}} or {@code {"prices": { "2024-05-01": 2300 }}} style payloads.
     */
    private NavigableMap<LocalDate, BigDecimal> tryParseHistoryAsDatePriceObject(JsonNode root) {
        NavigableMap<LocalDate, BigDecimal> map = new TreeMap<>();
        if (root == null || !root.isObject() || root.isArray()) {
            return map;
        }
        JsonNode bucket = root;
        if (root.has("prices") && root.get("prices").isObject()) {
            bucket = root.get("prices");
        } else if (root.has("data") && root.get("data").isObject() && !root.get("data").has("symbol")) {
            bucket = root.get("data");
        }
        if (bucket == root && root.size() > 0) {
            Iterator<String> names = root.fieldNames();
            if (names.hasNext()) {
                String first = names.next();
                if (root.get(first).isObject() || root.get(first).isArray()) {
                    return map;
                }
            }
        }
        var fields = bucket.fields();
        while (fields.hasNext()) {
            var e = fields.next();
            if (!e.getValue().isNumber()) {
                continue;
            }
            try {
                String key = e.getKey();
                if (key.length() >= 10 && key.charAt(4) == '-' && key.charAt(7) == '-') {
                    LocalDate d = LocalDate.parse(key.substring(0, 10));
                    BigDecimal oz = e.getValue().decimalValue();
                    map.put(d, perGramFromTroyOunce(oz));
                }
            } catch (DateTimeParseException ignored) {
                // skip non-date keys
            }
        }
        return map;
    }

    private static JsonNode resolveHistoryArray(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (!root.isObject()) {
            return null;
        }
        JsonNode preferred = firstArrayField(root, "data", "history", "prices", "items", "results", "rows", "records", "series", "values");
        if (preferred != null) {
            return preferred;
        }
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            if (e.getValue().isArray()) {
                return e.getValue();
            }
        }
        return null;
    }

    private static JsonNode firstArrayField(JsonNode root, String... names) {
        for (String n : names) {
            if (root.has(n) && root.get(n).isArray()) {
                return root.get(n);
            }
        }
        return null;
    }

    private LocalDate parseHistoryDate(JsonNode node) {
        if (node.has("period") && node.get("period").isObject()) {
            LocalDate nested = parseHistoryDate(node.get("period"));
            if (nested != null) {
                return nested;
            }
        }
        if (node.hasNonNull("day")) {
            JsonNode dnode = node.get("day");
            if (dnode.isTextual()) {
                try {
                    String raw = dnode.asText();
                    if (raw.length() >= 10) {
                        return LocalDate.parse(raw.substring(0, 10));
                    }
                    return LocalDate.parse(raw);
                } catch (DateTimeParseException ex) {
                    return null;
                }
            }
            if (dnode.isIntegralNumber()) {
                try {
                    int y = node.path("year").asInt(0);
                    int m = node.path("month").asInt(0);
                    if (y > 0 && m > 0) {
                        return LocalDate.of(y, m, dnode.intValue());
                    }
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        if (node.hasNonNull("date")) {
            try {
                String raw = node.get("date").asText();
                if (raw.length() >= 10) {
                    return LocalDate.parse(raw.substring(0, 10));
                }
                return LocalDate.parse(raw);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
        long ts = epochSeconds(node, "timestamp");
        if (ts > 0) {
            return instantFromEpoch(ts).atZone(ZoneOffset.UTC).toLocalDate();
        }
        if (node.hasNonNull("time")) {
            try {
                return Instant.parse(node.get("time").asText()).atZone(ZoneOffset.UTC).toLocalDate();
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
        if (node.hasNonNull("updatedAt")) {
            try {
                return Instant.parse(node.get("updatedAt").asText()).atZone(ZoneOffset.UTC).toLocalDate();
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
        ts = epochSeconds(node, "startTimestamp");
        if (ts > 0) {
            return instantFromEpoch(ts).atZone(ZoneOffset.UTC).toLocalDate();
        }
        ts = epochSeconds(node, "endTimestamp");
        if (ts > 0) {
            return instantFromEpoch(ts).atZone(ZoneOffset.UTC).toLocalDate();
        }
        if (node.has("year") && node.has("month")) {
            try {
                int y = node.get("year").asInt();
                int m = node.get("month").asInt();
                int d = node.path("day").asInt(1);
                return LocalDate.of(y, m, d);
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    private static long epochSeconds(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return -1;
        }
        JsonNode n = node.get(field);
        if (n.isIntegralNumber()) {
            long raw = n.longValue();
            return raw > 1_000_000_000_000L ? raw / 1000 : raw;
        }
        if (n.isFloatingPointNumber()) {
            double d = n.doubleValue();
            long raw = (long) d;
            return raw > 1_000_000_000_000L ? raw / 1000 : raw;
        }
        if (n.isTextual()) {
            try {
                long raw = Long.parseLong(n.asText().trim());
                return raw > 1_000_000_000_000L ? raw / 1000 : raw;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private static Instant instantFromEpoch(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds);
    }

    /** Spot quotes and history aggregates (gold-api.com uses snake_case in history). */
    private BigDecimal pickPricePerOz(JsonNode node) {
        BigDecimal v = pickPricePerOzFlat(node);
        if (v != null) {
            return v;
        }
        for (String nest : new String[] {"values", "metrics", "data", "aggregations", "aggregation", "quote"}) {
            if (node.has(nest) && node.get(nest).isObject()) {
                v = pickPricePerOzFlat(node.get(nest));
                if (v != null) {
                    return v;
                }
            }
        }
        return pickPriceByHeuristic(node);
    }

    private BigDecimal pickPricePerOzFlat(JsonNode node) {
        for (String f : new String[] {
                "avg_price", "avgPrice", "average_price", "averagePrice", "mean_price", "meanPrice",
                "max_price", "maxPrice", "min_price", "minPrice",
                "price", "close", "open", "last", "value", "amount", "avg", "mean"
        }) {
            BigDecimal v = decimal(node, f);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static BigDecimal pickPriceByHeuristic(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String key = e.getKey().toLowerCase();
            JsonNode val = e.getValue();
            if (!val.isNumber()) {
                continue;
            }
            if (key.contains("price") || key.equals("avg") || key.equals("average") || key.equals("mean")) {
                return val.decimalValue();
            }
        }
        return null;
    }

    private static BigDecimal perGramFromTroyOunce(BigDecimal pricePerOz) {
        return pricePerOz.divide(GRAMS_PER_TROY_OUNCE, 8, RoundingMode.HALF_UP);
    }

    private static BigDecimal perKilogramFromGram(BigDecimal perGram) {
        return perGram.multiply(BigDecimal.valueOf(1000)).setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(JsonNode root, String field) {
        if (!root.has(field) || root.get(field).isNull()) {
            return null;
        }
        JsonNode n = root.get(field);
        if (n.isNumber()) {
            return n.decimalValue();
        }
        try {
            return new BigDecimal(n.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("Metals API is not configured. Set mely.goldapi.base-url (default https://api.gold-api.com).");
        }
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        return currency.trim().toUpperCase();
    }

    private void sleepBetweenCalls() {
        long ms = properties.getRequestDelayMs();
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
