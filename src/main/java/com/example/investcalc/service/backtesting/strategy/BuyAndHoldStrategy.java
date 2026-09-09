package com.example.investcalc.service.backtesting.strategy;

import com.example.investcalc.repository.entity.Candle;

import java.util.ArrayList;
import java.util.List;

public class BuyAndHoldStrategy implements TradingStrategy {
    @Override
    public String getName() {
        return "BUY_AND_HOLD";
    }
    @Override
    public List<Signal> generateSignals(List<Candle> candles) {
        List<Signal> signals = new ArrayList<>(candles.size());
        for (int i = 0; i < candles.size(); i++) {
            signals.add(i == 0 ? Signal.BUY : Signal.HOLD);
        }
        return signals;
    }
}