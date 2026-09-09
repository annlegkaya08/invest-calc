package com.example.investcalc.service.backtesting.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EquityPoint {
    private final LocalDate date;
    private final BigDecimal equity;

    public EquityPoint(LocalDate date, BigDecimal equity) {
        this.date = date;
        this.equity = equity;
    }

    public LocalDate getDate() { return date; }
    public BigDecimal getEquity() { return equity; }
}