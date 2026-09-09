package com.example.investcalc.service.backtesting.strategy;

import org.springframework.stereotype.Component;

@Component
public class StrategyFactory {

    public TradingStrategy create(String name) {
        if (name == null || name.isBlank()) {
            return new BuyAndHoldStrategy();
        }
        return switch (name.trim().toUpperCase()) {
            case "MA_CROSSOVER", "MA", "MACROSS" -> new MACrossoverStrategy(10, 30);
            case "BUY_AND_HOLD", "B&H", "HOLD" -> new BuyAndHoldStrategy();
            default -> throw new IllegalArgumentException("Unknown strategy: " + name);
        };
    }
}