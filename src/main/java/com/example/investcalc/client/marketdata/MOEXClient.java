// client/marketdata/MoexDataClient.java
package com.example.investcalc.client.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.investcalc.repository.entity.Candle;
import com.example.investcalc.repository.entity.TickerMetadata;
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
@ConditionalOnProperty(name = "marketdata.provider", havingValue = "moex")
public class MOEXClient implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(MOEXClient.class);
    private static final String BASE = "https://iss.moex.com/iss";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestClient restClient;

    public MOEXClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE).build();
    }

    @Override
    public List<Candle> getCandles(String ticker, LocalDate from, LocalDate to) {
        String url = UriComponentsBuilder
                .fromPath("/engines/stock/markets/shares/securities/{secid}/candles.json")
                .queryParam("from", from.format(FMT))
                .queryParam("till", to.format(FMT))
                .queryParam("interval", 24)          // дневные свечи
                .queryParam("start", 0)
                .buildAndExpand(ticker.toUpperCase())
                .toUriString();

        try {
            JsonNode root = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(JsonNode.class);

            return parseCandles(ticker.toUpperCase(), root);
        } catch (Exception e) {
            log.error("MOEX candles error for {}: {}", ticker, e.getMessage());
            return List.of();
        }
    }

    private List<Candle> parseCandles(String ticker, JsonNode root) {
        if (root == null || !root.has("candles")) {
            return List.of();
        }

        JsonNode columns = root.path("candles").path("columns");
        JsonNode data = root.path("candles").path("data");

        Map<String, Integer> colIdx = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            colIdx.put(columns.get(i).asText(), i);
        }

        List<Candle> result = new ArrayList<>();
        for (JsonNode row : data) {
            try {
                LocalDate date = LocalDate.parse(row.get(colIdx.get("begin")).asText().substring(0, 10));
                BigDecimal open = bd(row, colIdx, "open");
                BigDecimal close = bd(row, colIdx, "close");
                BigDecimal high = bd(row, colIdx, "high");
                BigDecimal low = bd(row, colIdx, "low");
                long volume = row.get(colIdx.getOrDefault("volume", 0)).asLong(0);

                result.add(new Candle(ticker, date, open, high, low, close, volume));
            } catch (Exception ignored) {
                // пропускаем битую строку
            }
        }
        return result;
    }

    private BigDecimal bd(JsonNode row, Map<String, Integer> colIdx, String name) {
        Integer idx = colIdx.get(name);
        if (idx == null || row.get(idx).isNull()) {
            return BigDecimal.ZERO;
        }
        return row.get(idx).decimalValue();
    }

    @Override
    public Optional<TickerMetadata> getTickerMetadata(String ticker) {
        String url = "/securities/" + ticker.toUpperCase() + ".json";

        try {
            JsonNode root = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(JsonNode.class);

            return parseMetadata(ticker.toUpperCase(), root);
        } catch (Exception e) {
            log.error("MOEX metadata error for {}: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<TickerMetadata> parseMetadata(String ticker, JsonNode root) {
        // description блок
        JsonNode desc = root.path("description").path("data");
        if (desc.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> fields = new HashMap<>();
        for (JsonNode row : desc) {
            fields.put(row.get(0).asText(), row.get(2).asText());
        }

        TickerMetadata meta = new TickerMetadata();
        meta.setTicker(ticker);
        meta.setName(fields.getOrDefault("SHORTNAME", fields.getOrDefault("NAME", ticker)));
        meta.setIsin(fields.get("ISIN"));
        meta.setCurrency(fields.getOrDefault("FACEUNIT", "RUB"));
        meta.setSector(mapSector(fields.getOrDefault("SECTOR", "UNKNOWN")));
        meta.setBoard("TQBR");

        return Optional.of(meta);
    }

    private String mapSector(String raw) {
        return switch (raw.toUpperCase()) {
            //other categories
            case "OIL", "GAS", "OIL&GAS" -> "OIL_GAS";
            case "BANKS", "FINANCE" -> "FINANCE";
            case "METALS", "MINING" -> "METALS";
            case "IT", "TECH" -> "IT";
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
            JsonNode root = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(JsonNode.class);

            List<TickerMetadata> result = new ArrayList<>();
            JsonNode securities = root.path("securities").path("data");
            JsonNode columns = root.path("securities").path("columns");

            Map<String, Integer> colIdx = new HashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                colIdx.put(columns.get(i).asText(), i);
            }

            for (JsonNode row : securities) {
                String secid = row.get(colIdx.get("secid")).asText();
                String shortname = row.get(colIdx.get("shortname")).asText();
                if (secid.length() <= 5) {
                    TickerMetadata m = new TickerMetadata(secid, shortname, "UNKNOWN");
                    result.add(m);
                }
                if (result.size() >= 15) break;
            }
            return result;
        } catch (Exception e) {
            log.error("MOEX search error: {}", e.getMessage());
            return List.of();
        }
    }
}