package com.example.investcalc.service.marketdata;

import com.example.investcalc.client.marketdata.MarketDataProvider;
import com.example.investcalc.client.marketdata.MOEXClient;
import com.example.investcalc.client.marketdata.MockDataProvider;
import com.example.investcalc.config.CacheConfig;
import com.example.investcalc.repository.CandleRepository;
import com.example.investcalc.repository.TickerMetadataRepository;
import com.example.investcalc.repository.entity.Candle;
import com.example.investcalc.repository.entity.TickerMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read path: DB first → API for missing dates → persist gaps into candle.
 * Metadata is cached.
 */
@Service
@Primary
public class MarketDataService implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final MarketDataProvider apiClient;
    private final CandleRepository candleRepository;
    private final TickerMetadataRepository tickerMetadataRepository;

    public MarketDataService(
            ObjectProvider<MOEXClient> moexProvider,
            ObjectProvider<MockDataProvider> mockProvider,
            CandleRepository candleRepository,
            TickerMetadataRepository tickerMetadataRepository,
            @Value("${marketdata.provider:moex}") String provider) {

        MarketDataProvider chosen;
        if ("mock".equalsIgnoreCase(provider)) {
            chosen = mockProvider.getIfAvailable();
        } else {
            chosen = moexProvider.getIfAvailable();
            if (chosen == null) {
                chosen = mockProvider.getIfAvailable();
            }
        }
        if (chosen == null) {
            throw new IllegalStateException("No MarketDataProvider (MOEX/Mock) available");
        }
        this.apiClient = chosen;
        this.candleRepository = candleRepository;
        this.tickerMetadataRepository = tickerMetadataRepository;
        log.info("MarketDataService using API client: {}", chosen.getClass().getSimpleName());
    }

    /**
     * DB-first candles: load existing range, request missing gaps from API, save them.
     */
    @Override
    @Transactional
    public List<Candle> getCandles(String ticker, LocalDate from, LocalDate to) {
        String key = ticker.toUpperCase().trim();
        List<Candle> fromDb = candleRepository.findByTickerAndDateBetweenOrderByDateAsc(key, from, to);

        Set<LocalDate> existing = fromDb.stream()
                .map(Candle::getDate)
                .collect(Collectors.toCollection(HashSet::new));

        if (!fromDb.isEmpty() && isRangeCovered(fromDb, from, to)) {
            return fromDb;
        }

        List<Candle> fromApi = apiClient.getCandles(key, from, to);
        if (fromApi.isEmpty()) {
            return fromDb;
        }

        List<Candle> toSave = new ArrayList<>();
        for (Candle c : fromApi) {
            if (!existing.contains(c.getDate())) {
                toSave.add(c);
            }
        }
        if (!toSave.isEmpty()) {
            candleRepository.saveAll(toSave);
            log.info("Persisted {} new candles for {} [{}, {}]", toSave.size(), key, from, to);
        }

        Map<LocalDate, Candle> merged = new TreeMap<>();
        fromDb.forEach(c -> merged.put(c.getDate(), c));
        fromApi.forEach(c -> merged.putIfAbsent(c.getDate(), c));
        return new ArrayList<>(merged.values());
    }

    private boolean isRangeCovered(List<Candle> candles, LocalDate from, LocalDate to) {
        if (candles.isEmpty()) {
            return false;
        }
        LocalDate first = candles.getFirst().getDate();
        LocalDate last = candles.getLast().getDate();
        long calendarDays = Math.max(1, to.toEpochDay() - from.toEpochDay() + 1);
        long expectedTradingApprox = Math.max(1, (long) (calendarDays * 0.65));
        boolean spans = !first.isAfter(from.plusDays(5)) && !last.isBefore(to.minusDays(5));
        return spans && candles.size() >= expectedTradingApprox;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.TICKER_METADATA, key = "#ticker.toUpperCase()")
    @Transactional
    public Optional<TickerMetadata> getTickerMetadata(String ticker) {
        String key = ticker.toUpperCase().trim();
        Optional<TickerMetadata> fromDb = tickerMetadataRepository.findByTickerIgnoreCase(key);
        if (fromDb.isPresent()) {
            TickerMetadata m = fromDb.get();
            if (m.getSector() != null && !"UNKNOWN".equalsIgnoreCase(m.getSector())) {
                return fromDb;
            }
        }

        Optional<TickerMetadata> fromApi = apiClient.getTickerMetadata(key);
        if (fromApi.isPresent()) {
            TickerMetadata meta = fromApi.get();
            TickerMetadata toSave = fromDb.orElseGet(() -> {
                TickerMetadata m = new TickerMetadata();
                m.setTicker(key);
                return m;
            });
            toSave.setName(meta.getName() != null ? meta.getName() : key);
            toSave.setSector(meta.getSector() != null ? meta.getSector() : "UNKNOWN");
            toSave.setCurrency(meta.getCurrency() != null ? meta.getCurrency() : "RUB");
            toSave.setBoard(meta.getBoard() != null ? meta.getBoard() : "TQBR");
            toSave.setIsin(meta.getIsin());
            toSave.setUpdatedAt(Instant.now());
            tickerMetadataRepository.save(toSave);
            return Optional.of(toSave);
        }
        return fromDb;
    }

    @Override
    public List<TickerMetadata> searchTickers(String query) {
        List<TickerMetadata> fromDb = tickerMetadataRepository.search(query);
        if (!fromDb.isEmpty()) {
            return fromDb.stream().limit(15).toList();
        }
        return apiClient.searchTickers(query);
    }

    /**
     * Used by night scheduler: force-refresh latest candles for known tickers.
     */
    @Transactional
    public int refreshRecentCandles(String ticker, LocalDate from, LocalDate to) {
        String key = ticker.toUpperCase().trim();
        List<Candle> fromApi = apiClient.getCandles(key, from, to);
        if (fromApi.isEmpty()) {
            return 0;
        }
        int saved = 0;
        for (Candle c : fromApi) {
            if (!candleRepository.existsByTickerAndDate(key, c.getDate())) {
                candleRepository.save(c);
                saved++;
            }
        }
        return saved;
    }
}
