package com.example.investcalc.exception;

import com.example.investcalc.api.exception.ApiErrorCode;

public class TickerNotFoundException extends BusinessException {

    private final String ticker;

    public TickerNotFoundException(String ticker) {
        super(ApiErrorCode.TICKER_NOT_FOUND, "Тикер не найден: " + ticker);
        this.ticker = ticker;
    }

    public String getTicker() {
        return ticker;
    }
}
