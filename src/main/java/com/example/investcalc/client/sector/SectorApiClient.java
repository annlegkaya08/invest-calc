package com.example.investcalc.client.sector;

import com.example.investcalc.client.retry.RetryConfig;
import com.example.investcalc.config.CacheConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class SectorApiClient {

    private static final Logger log = LoggerFactory.getLogger(SectorApiClient.class);
    private static final String BASE = "https://iss.moex.com/iss";

    /** MOEX sector index SECID � internal sector code */
    private static final Map<String, String> INDEX_TO_SECTOR = Map.ofEntries(
            Map.entry("MOEXOG", "OIL_GAS"),
            Map.entry("MOEXFN", "FINANCE"),
            Map.entry("MOEXMM", "METALS"),
            Map.entry("MOEXIT", "IT"),
            Map.entry("MOEXCN", "CONSUMER"),
            Map.entry("MOEXEU", "UTILITIES"),
            Map.entry("MOEXCH", "CHEMICALS"),
            Map.entry("MOEXTN", "TRANSPORT"),
            Map.entry("MOEXRE", "REAL_ESTATE"),
            Map.entry("MOEXTE", "TELECOM"),
            Map.entry("MOEXSC", "MATERIALS")
    );

    private final RestClient restClient;
    private final RetryConfig retryConfig;

    public SectorApiClient(RestClient.Builder builder, RetryConfig retryConfig) {
        this.restClient = builder.baseUrl(BASE).build();
        this.retryConfig = retryConfig;
    }

    /**
     * Full map ticker � sector built from all known sector indices.
     * Cached for 12h.
     */
    @Cacheable(cacheNames = CacheConfig.SECTOR_MAP, key = "'all'")
    public Map<String, String> loadAllSectorMappings() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> e : INDEX_TO_SECTOR.entrySet()) {
            String index = e.getKey();
            String sector = e.getValue();
            try {
                Set<String> constituents = fetchIndexConstituents(index);
                for (String ticker : constituents) {
                    result.putIfAbsent(ticker, sector);
                }
                log.info("Sector index {}: {} tickers � {}", index, constituents.size(), sector);
            } catch (Exception ex) {
                log.warn("Failed to load constituents of {}: {}", index, ex.getMessage());
            }
        }
        log.info("Total sector mappings loaded: {}", result.size());
        return Collections.unmodifiableMap(result);
    }

    /**
     * Sector for a single ticker (uses cached full map).
     */
    public Optional<String> resolveSector(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return Optional.empty();
        }
        String sector = loadAllSectorMappings().get(ticker.toUpperCase().trim());
        return Optional.ofNullable(sector);
    }

    private Set<String> fetchIndexConstituents(String indexSecid) {
        // https://iss.moex.com/iss/statistics/engines/stock/markets/index/analytics/MOEXOG.json
        // Alternative: /iss/engines/stock/markets/index/securities/{index}/analytics.json
        // Most reliable for composition: /iss/statistics/engines/stock/markets/index/analytics/{index}.json
        // columns include ticker / secid

        String url = "/statistics/engines/stock/markets/index/analytics/" + indexSecid + ".json"
                + "?iss.meta=off&iss.only=analytics&limit=100";

        JsonNode root = retryConfig.executeWithRetry(
                () -> restClient.get().uri(url).retrieve().body(JsonNode.class),
                "sector-index-" + indexSecid
        );

        Set<String> tickers = new LinkedHashSet<>();
        if (root == null) {
            return tickers;
        }

        JsonNode analytics = root.path("analytics");
        JsonNode columns = analytics.path("columns");
        JsonNode data = analytics.path("data");

        if (columns.isMissingNode() || data.isMissingNode() || !data.isArray()) {
            // fallback: securities list of the index
            return fetchIndexSecuritiesFallback(indexSecid);
        }

        Map<String, Integer> colIdx = columnIndex(columns);
        Integer tickerCol = firstPresent(colIdx, "ticker", "secid", "SECID", "TICKER");
        if (tickerCol == null) {
            return fetchIndexSecuritiesFallback(indexSecid);
        }

        for (JsonNode row : data) {
            if (row.isArray() && row.size() > tickerCol) {
                String t = row.get(tickerCol).asText(null);
                if (t != null && !t.isBlank() && !t.equalsIgnoreCase(indexSecid)) {
                    tickers.add(t.toUpperCase());
                }
            }
        }
        return tickers;
    }

    private Set<String> fetchIndexSecuritiesFallback(String indexSecid) {
        String url = "/engines/stock/markets/index/securities/" + indexSecid + "/analytics.json"
                + "?iss.meta=off";

        try {
            JsonNode root = retryConfig.executeWithRetry(
                    () -> restClient.get().uri(url).retrieve().body(JsonNode.class),
                    "sector-fallback-" + indexSecid
            );
            Set<String> tickers = new LinkedHashSet<>();
            if (root == null) {
                return tickers;
            }
            JsonNode block = root.path("analytics");
            if (block.isMissingNode()) {
                block = root.path("securities");
            }
            JsonNode columns = block.path("columns");
            JsonNode data = block.path("data");
            Map<String, Integer> colIdx = columnIndex(columns);
            Integer tickerCol = firstPresent(colIdx, "ticker", "secid", "SECID");
            if (tickerCol == null || !data.isArray()) {
                return tickers;
            }
            for (JsonNode row : data) {
                if (row.isArray() && row.size() > tickerCol) {
                    String t = row.get(tickerCol).asText(null);
                    if (t != null && !t.isBlank()) {
                        tickers.add(t.toUpperCase());
                    }
                }
            }
            return tickers;
        } catch (Exception e) {
            log.warn("Fallback constituents for {} failed: {}", indexSecid, e.getMessage());
            return Set.of();
        }
    }

    private static Map<String, Integer> columnIndex(JsonNode columns) {
        Map<String, Integer> map = new HashMap<>();
        if (columns == null || !columns.isArray()) {
            return map;
        }
        for (int i = 0; i < columns.size(); i++) {
            map.put(columns.get(i).asText(), i);
        }
        return map;
    }

    private static Integer firstPresent(Map<String, Integer> colIdx, String... names) {
        for (String n : names) {
            if (colIdx.containsKey(n)) {
                return colIdx.get(n);
            }
        }
        return null;
    }
}
