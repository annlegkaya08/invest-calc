package com.example.investcalc.service.portfolio.model;

import java.math.BigDecimal;

public class WeightedPosition {

    private final String ticker;
    private final BigDecimal weight;
    private String sector;          // заполняется позже

    public WeightedPosition(String ticker, BigDecimal weight) {
        this.ticker = ticker.toUpperCase();
        this.weight = weight;
    }

    public String getTicker() { return ticker; }
    public BigDecimal getWeight() { return weight; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
}