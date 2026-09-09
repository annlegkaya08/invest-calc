package com.example.investcalc.service.backtesting.strategy;

import com.example.investcalc.repository.entity.Candle;
import com.example.investcalc.service.backtesting.calculator.MovingAverageCalculator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MACrossoverStrategy implements TradingStrategy {
    private final int fastPeriod;
    private final int slowPeriod;

    public MACrossoverStrategy(int fastPeriod, int slowPeriod) {
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException("fastPeriod must be < slowPeriod");
        }
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
    }

    public MACrossoverStrategy() {
        this(10, 30);
    }

    @Override
    public String getName() {
        return "MA_CROSSOVER";
    }


    @Override
    public List<Signal> generateSignals(List<Candle> candles) {
        List<BigDecimal> closes = candles.stream().map(Candle::getClose).toList();
        List<BigDecimal> fast = MovingAverageCalculator.sma(closes, fastPeriod);
        List<BigDecimal> slow = MovingAverageCalculator.sma(closes, slowPeriod);

        List<Signal> signals = new ArrayList<>(candles.size());
        for (int i = 0; i < candles.size(); i++) {
            if (fast.get(i) == null || slow.get(i) == null) {
                signals.add(Signal.HOLD);
                continue;
            }
            if (i == 0 || fast.get(i - 1) == null || slow.get(i - 1) == null) {
                signals.add(Signal.HOLD);
                continue;
            }

            boolean crossUp = fast.get(i - 1).compareTo(slow.get(i - 1)) <= 0
                    && fast.get(i).compareTo(slow.get(i)) > 0;
            boolean crossDown = fast.get(i - 1).compareTo(slow.get(i - 1)) >= 0
                    && fast.get(i).compareTo(slow.get(i)) < 0;

            if (crossUp) {
                signals.add(Signal.BUY);
            } else if (crossDown) {
                signals.add(Signal.SELL);
            } else {
                signals.add(Signal.HOLD);
            }
        }
        return signals;
    }
}