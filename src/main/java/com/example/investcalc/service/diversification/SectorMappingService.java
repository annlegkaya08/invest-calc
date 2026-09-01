package com.example.investcalc.service.diversification;

import com.example.investcalc.client.marketdata.MarketDataProvider;
import com.example.investcalc.repository.TickerMetadataRepository;
import com.example.investcalc.repository.entity.TickerMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class SectorMappingService {

    private static final Logger log = LoggerFactory.getLogger(SectorMappingService.class);

    private final TickerMetadataRepository tickerMetadataRepository;
    private final MarketDataProvider marketDataProvider;

    public SectorMappingService(TickerMetadataRepository tickerMetadataRepository,
                                MarketDataProvider marketDataProvider) {
        this.tickerMetadataRepository = tickerMetadataRepository;
        this.marketDataProvider = marketDataProvider;
    }

    @Transactional
    public String resolveSector(String ticker) {
        String key = ticker.toUpperCase();

        Optional<TickerMetadata> fromDb = tickerMetadataRepository.findByTickerIgnoreCase(key);
        if (fromDb.isPresent() && fromDb.get().getSector() != null
                && !"UNKNOWN".equalsIgnoreCase(fromDb.get().getSector())) {
            return fromDb.get().getSector();
        }

        // fallback → API
        Optional<TickerMetadata> fromApi = marketDataProvider.getTickerMetadata(key);
        if (fromApi.isPresent()) {
            TickerMetadata meta = fromApi.get();
            meta.setUpdatedAt(Instant.now());
            tickerMetadataRepository.save(meta);
            log.info("Sector for {} resolved via API: {}", key, meta.getSector());
            return meta.getSector() != null ? meta.getSector() : "UNKNOWN";
        }

        log.warn("Sector for {} not found, using UNKNOWN", key);
        return "UNKNOWN";
    }

    @Transactional(readOnly = true)
    public Optional<TickerMetadata> getMetadata(String ticker) {
        return tickerMetadataRepository.findByTickerIgnoreCase(ticker.toUpperCase())
                .or(() -> marketDataProvider.getTickerMetadata(ticker.toUpperCase()));
    }
}