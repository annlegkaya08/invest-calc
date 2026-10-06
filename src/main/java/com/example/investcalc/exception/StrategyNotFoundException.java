package com.example.investcalc.exception;

import com.example.investcalc.api.exception.ApiErrorCode;

import java.util.Collection;

public class StrategyNotFoundException extends BusinessException {

    public StrategyNotFoundException(String strategy, Collection<String> supported) {
        super(ApiErrorCode.UNKNOWN_STRATEGY,
                "Неизвестная стратегия: " + strategy,
                java.util.List.of("Доступные стратегии: " + String.join(", ", supported)));
    }
}
