package com.example.investcalc.api.controller;

import com.example.investcalc.api.dto.response.TickerSuggestion;
import com.example.investcalc.client.marketdata.MarketDataProvider;
import com.example.investcalc.repository.entity.TickerMetadata;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickers")
public class TickerController {

    private final MarketDataProvider marketDataProvider;

    public TickerController(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    @GetMapping
    public List<TickerSuggestion> search(@RequestParam(name = "q", defaultValue = "") String query) {
        if (query.isBlank()) {
            return List.of();
        }

        List<TickerMetadata> found = marketDataProvider.searchTickers(query.trim());
        return found.stream()
                .map(t -> new TickerSuggestion(
                        t.getTicker(),
                        t.getName(),
                        t.getSector() != null ? t.getSector() : "UNKNOWN"
                ))
                .toList();
    }
}