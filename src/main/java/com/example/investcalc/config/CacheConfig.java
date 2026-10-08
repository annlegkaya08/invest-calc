package com.example.investcalc.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * In-memory cache for ticker metadata (and optionally sectors).
 * TTL keeps sector mappings reasonably fresh without hammering MOEX.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TICKER_METADATA = "tickerMetadata";
    public static final String SECTOR_MAP = "sectorMap";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(TICKER_METADATA, SECTOR_MAP);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(12, TimeUnit.HOURS)
                .recordStats());
        return manager;
    }
}
