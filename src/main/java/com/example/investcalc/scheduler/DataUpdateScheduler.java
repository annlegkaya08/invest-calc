package com.example.investcalc.scheduler;

import com.example.investcalc.repository.TickerMetadataRepository;
import com.example.investcalc.repository.entity.TickerMetadata;
import com.example.investcalc.service.marketdata.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Nightly job: refresh recent candles for every ticker stored in ticker_metadata.
 */
@Component
public class DataUpdateScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataUpdateScheduler.class);

    private final TickerMetadataRepository tickerMetadataRepository;
    private final MarketDataService marketDataService;

    @Value("${marketdata.scheduler.lookback-days:30}")
    private int lookbackDays;

    @Value("${marketdata.scheduler.enabled:true}")
    private boolean enabled;

    public DataUpdateScheduler(TickerMetadataRepository tickerMetadataRepository,
                               MarketDataService marketDataService) {
        this.tickerMetadataRepository = tickerMetadataRepository;
        this.marketDataService = marketDataService;
    }

    /** Every day at 23:00 server time */
    @Scheduled(cron = "${marketdata.scheduler.cron:0 0 23 * * *}")
    public void updateDailyCandles() {
        if (!enabled) {
            log.debug("DataUpdateScheduler disabled");
            return;
        }

        List<TickerMetadata> tickers = tickerMetadataRepository.findAll();
        if (tickers.isEmpty()) {
            log.info("DataUpdateScheduler: no tickers in DB, skip");
            return;
        }

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(lookbackDays);
        log.info("DataUpdateScheduler: updating candles for {} tickers [{}, {}]",
                tickers.size(), from, to);

        int totalSaved = 0;
        int failed = 0;
        for (TickerMetadata meta : tickers) {
            try {
                int saved = marketDataService.refreshRecentCandles(meta.getTicker(), from, to);
                totalSaved += saved;
            } catch (Exception e) {
                failed++;
                log.warn("Failed to update {}: {}", meta.getTicker(), e.getMessage());
            }
        }
        log.info("DataUpdateScheduler done: saved={}, failed={}, tickers={}",
                totalSaved, failed, tickers.size());
    }
}
