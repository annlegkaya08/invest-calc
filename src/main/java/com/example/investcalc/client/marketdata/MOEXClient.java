package com.example.investcalc.client.marketdata;

import com.example.investcalc.client.retry.RetryConfig;
import com.example.investcalc.client.sector.SectorApiClient;
import com.example.investcalc.repository.entity.Candle;
import com.example.investcalc.repository.entity.TickerMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@ConditionalOnProperty(name = "marketdata.provider", havingValue = "moex", matchIfMissing = true)
public class MOEXClient implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(MOEXClient.class);
    private static final String BASE = "https://iss.moex.com/iss";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int PAGE_SIZE = 500;

    private final RestClient restClient;
    private final RetryConfig retryConfig;
    private final SectorApiClient sectorApiClient;

    public MOEXClient(RestClient.Builder builder,
                      RetryConfig retryConfig,
                      SectorApiClient sectorApiClient) {
        this.restClient = builder.baseUrl(BASE).build();
        this.retryConfig = retryConfig;
        this.sectorApiClient = sectorApiClient;
    }

    @Override
    public List<Candle> getCandles(String ticker, LocalDate from, LocalDate to) {
        String secid = ticker.toUpperCase().trim();
        List<Candle> all = new ArrayList<>();
        int start = 0;

        while (true) {
            final int pageStart = start;
            String url = UriComponentsBuilder
                    .fromPath("/engines/stock/markets/shares/securities/{secid}/candles.json")
                    .queryParam("from", from.format(FMT))
                    .queryParam("till", to.format(FMT))
                    .queryParam("interval", 24)
                    .queryParam("start", pageStart)
                    .buildAndExpand(secid)
                    .toUriString();

            try {
                JsonNode root = retryConfig.executeWithRetry(
                        () -> restClient.get().uri(url).retrieve().body(JsonNode.class),
                        "candles-" + secid + "-start-" + pageStart
                );
                List<Candle> page = parseCandles(secid, root);
                all.addAll(page);
                if (page.size() < PAGE_SIZE) {
                    break;
                }
                start += PAGE_SIZE;
            } catch (Exception e) {
                log.error("MOEX candles error for {} (start={}): {}", secid, pageStart, e.getMessage());
                break;
            }
        }

        all.sort(Comparator.comparing(Candle::getDate));
        return all;
    }

    private List<Candle> parseCandles(String ticker, JsonNode root) {
        if (root == null || !root.has("candles")) {
            return List.of();
        }

        JsonNode columns = root.path("candles").path("columns");
        JsonNode data = root.path("candles").path("data");
        if (!data.isArray() || data.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> colIdx = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            colIdx.put(columns.get(i).asText(), i);
        }

        List<Candle> result = new ArrayList<>(data.size());
        for (JsonNode row : data) {
            try {
                LocalDate date = LocalDate.parse(row.get(colIdx.get("begin")).asText().substring(0, 10));
                BigDecimal open = bd(row, colIdx, "open");
                BigDecimal close = bd(row, colIdx, "close");
                BigDecimal high = bd(row, colIdx, "high");
                BigDecimal low = bd(row, colIdx, "low");
                long volume = 0L;
                Integer volIdx = colIdx.get("volume");
                if (volIdx != null && !row.get(volIdx).isNull()) {
                    volume = row.get(volIdx).asLong(0);
                }
                result.add(new Candle(ticker, date, open, high, low, close, volume));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private BigDecimal bd(JsonNode row, Map<String, Integer> colIdx, String name) {
        Integer idx = colIdx.get(name);
        if (idx == null || row.get(idx) == null || row.get(idx).isNull()) {
            return BigDecimal.ZERO;
        }
        return row.get(idx).decimalValue();
    }

    @Override
    public Optional<TickerMetadata> getTickerMetadata(String ticker) {
        String secid = ticker.toUpperCase().trim();
        String url = "/securities/" + secid + ".json?iss.meta=off";

        try {
            JsonNode root = retryConfig.executeWithRetry(
                    () -> restClient.get().uri(url).retrieve().body(JsonNode.class),
                    "metadata-" + secid
            );
            return parseMetadata(secid, root);
        } catch (Exception e) {
            log.error("MOEX metadata error for {}: {}", secid, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<TickerMetadata> parseMetadata(String ticker, JsonNode root) {
        if (root == null) {
            return Optional.empty();
        }
        JsonNode desc = root.path("description").path("data");
        if (desc.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> fields = new HashMap<>();
        for (JsonNode row : desc) {
            if (row.isArray() && row.size() >= 3) {
                fields.put(row.get(0).asText(), row.get(2).asText());
            }
        }

        TickerMetadata meta = new TickerMetadata();
        meta.setTicker(ticker);
        meta.setName(fields.getOrDefault("SHORTNAME", fields.getOrDefault("NAME", ticker)));
        meta.setIsin(fields.get("ISIN"));
        meta.setCurrency(fields.getOrDefault("FACEUNIT", "RUB"));
        meta.setBoard("TQBR");

        String sector = sectorApiClient.resolveSector(ticker)
                .orElseGet(() -> mapSector(fields.getOrDefault("SECTOR", "UNKNOWN")));
        meta.setSector(sector);

        return Optional.of(meta);
    }

    private String mapSector(String raw) {
        if (raw == null || raw.isBlank()) {
            return "UNKNOWN";
        }
        return switch (raw.toUpperCase()) {
            case "OIL", "GAS", "OIL&GAS", "OIL_GAS" -> "OIL_GAS";
            case "BANKS", "FINANCE", "FINANCIALS" -> "FINANCE";
            case "METALS", "MINING", "METALS_MINING" -> "METALS";
            case "IT", "TECH", "TECHNOLOGY" -> "IT";
            case "CONSUMER", "RETAIL" -> "CONSUMER";
            case "UTILITIES", "ENERGY" -> "UTILITIES";
            case "CHEMICALS", "CHEM" -> "CHEMICALS";
            case "TRANSPORT", "TRANSPORTATION" -> "TRANSPORT";
            case "TELECOM", "TELECOMMUNICATIONS" -> "TELECOM";
            case "REAL_ESTATE", "REALESTATE" -> "REAL_ESTATE";
            default -> "UNKNOWN";
        };
    }

    @Override
    public List<TickerMetadata> searchTickers(String query) {
        String url = UriComponentsBuilder
                .fromPath("/securities.json")
                .queryParam("q", query)
                .queryParam("iss.meta", "off")
                .toUriString();

        try {
            JsonNode root = retryConfig.executeWithRetry(
                    () -> restClient.get().uri(url).retrieve().body(JsonNode.class),
                    "search-" + query
            );

            List<TickerMetadata> result = new ArrayList<>();
            JsonNode securities = root.path("securities").path("data");
            JsonNode columns = root.path("securities").path("columns");

            Map<String, Integer> colIdx = new HashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                colIdx.put(columns.get(i).asText(), i);
            }

            Integer secidIdx = colIdx.get("secid");
            Integer shortnameIdx = colIdx.get("shortname");
            if (secidIdx == null) {
                return List.of();
            }

            for (JsonNode row : securities) {
                String secid = row.get(secidIdx).asText();
                String shortname = shortnameIdx != null ? row.get(shortnameIdx).asText(secid) : secid;
                // filter out long / bond-like codes
                if (secid.length() <= 6) {
                    String sector = sectorApiClient.resolveSector(secid).orElse("UNKNOWN");
                    result.add(new TickerMetadata(secid, shortname, sector));
                }
                if (result.size() >= 15) {
                    break;
                }
            }
            return result;
        } catch (Exception e) {
            log.error("MOEX search error: {}", e.getMessage());
            return List.of();
        }
    }
}
