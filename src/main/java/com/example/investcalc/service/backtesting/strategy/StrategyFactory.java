package com.example.investcalc.service.backtesting.strategy;

import com.example.investcalc.exception.StrategyNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StrategyFactory {

    private static final List<String> SUPPORTED = List.of("MA_CROSSOVER", "BUY_AND_HOLD");

    public TradingStrategy create(String name) {
        if (name == null || name.isBlank()) {
            return new BuyAndHoldStrategy();
        }
        return switch (name.trim().toUpperCase()) {
            case "MA_CROSSOVER", "MA", "MACROSS" -> new MACrossoverStrategy(10, 30);
            case "BUY_AND_HOLD", "B&H", "HOLD" -> new BuyAndHoldStrategy();
            default -> throw new StrategyNotFoundException(name, SUPPORTED);
        };
    }
}