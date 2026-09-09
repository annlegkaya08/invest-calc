package com.example.investcalc.service.backtesting.strategy;

import com.example.investcalc.repository.entity.Candle;

import java.util.List;

public interface TradingStrategy {

    String getName();
    List<Signal> generateSignals(List<Candle> candles);
}