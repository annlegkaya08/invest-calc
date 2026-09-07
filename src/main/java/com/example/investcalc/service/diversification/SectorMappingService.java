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
        String key = ticker.toUpperCase().trim();

        Optional<TickerMetadata> fromDb = tickerMetadataRepository.findByTickerIgnoreCase(key);

        // Берём из БД только если сектор нормальный
        if (fromDb.isPresent()) {
            String sector = fromDb.get().getSector();
            if (sector != null && !sector.isBlank() && !"UNKNOWN".equalsIgnoreCase(sector)) {
                return sector.toUpperCase();
            }
        }

        //Fallback? Тогда API / Mock
        Optional<TickerMetadata> fromApi = marketDataProvider.getTickerMetadata(key);
        if (fromApi.isPresent()) {
            TickerMetadata meta = fromApi.get();
            String sector = meta.getSector() != null ? meta.getSector().toUpperCase() : "UNKNOWN";

            TickerMetadata toSave = fromDb.orElseGet(() -> {
                TickerMetadata m = new TickerMetadata();
                m.setTicker(key);
                return m;
            });
            toSave.setName(meta.getName() != null ? meta.getName() : key);
            toSave.setSector(sector);
            toSave.setCurrency(meta.getCurrency() != null ? meta.getCurrency() : "RUB");
            toSave.setBoard(meta.getBoard() != null ? meta.getBoard() : "TQBR");
            toSave.setIsin(meta.getIsin());
            toSave.setUpdatedAt(Instant.now());
            tickerMetadataRepository.save(toSave);

            log.info("Sector resolved for {}: {}", key, sector);
            return sector;
        }

        log.warn("Sector NOT found for {}, using UNKNOWN. Check marketdata.provider and ticker list.", key);
        return "UNKNOWN";
    }
}