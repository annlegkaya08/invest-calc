package com.example.investcalc.client.marketdata;

import com.example.investcalc.repository.entity.Candle;
import com.example.investcalc.repository.entity.TickerMetadata;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketDataProvider {

    /**
     Historical daily candles
     **/
    List<Candle> getCandles(String ticker, LocalDate from, LocalDate to);

    Optional<TickerMetadata> getTickerMetadata(String ticker);

    /**
     Tiker search for autocomplete
     **/
    List<TickerMetadata> searchTickers(String query);
}