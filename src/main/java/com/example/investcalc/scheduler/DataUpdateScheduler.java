package com.example.investcalc.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DataUpdateScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataUpdateScheduler.class);

    @Scheduled(cron = "0 0 23 * * *") // каждый день в 23:00
    public void updateDailyCandles() {
        log.info("DataUpdateScheduler: updateDailyCandles() — stub, nothing to do yet");
        // TODO:
        // 1. взять список тикеров из TickerMetadataRepository
        // 2. для каждого вызвать MarketDataProvider.getCandles(...)
        // 3. сохранить новые свечи через CandleRepository
    }
}