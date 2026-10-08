package com.example.investcalc.service.portfolio.model;

import java.math.BigDecimal;
import java.util.Objects;

public class WeightedPosition {
    private final String ticker;
    private final BigDecimal weight;
    private String sector;

    public WeightedPosition(String ticker, BigDecimal weight) {
        this.ticker = ticker.toUpperCase();
        this.weight = weight;
    }

    public String getTicker() {
        return ticker;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    /** Immutable copy with a new weight (sector preserved). */
    public WeightedPosition withWeight(BigDecimal newWeight) {
        WeightedPosition copy = new WeightedPosition(this.ticker, newWeight);
        copy.setSector(this.sector);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeightedPosition that)) return false;
        return Objects.equals(ticker, that.ticker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker);
    }
}
