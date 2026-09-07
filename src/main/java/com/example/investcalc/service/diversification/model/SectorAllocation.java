package com.example.investcalc.service.diversification.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SectorAllocation {

    private final String sector;
    private final BigDecimal weight;
    private final int positionCount;

    public SectorAllocation(String sector, BigDecimal weight, int positionCount) {
        this.sector = sector;
        this.weight = weight.setScale(4, RoundingMode.HALF_UP);
        this.positionCount = positionCount;
    }

    public String getSector() {
        return sector;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public int getPositionCount() {
        return positionCount;
    }

    public BigDecimal getWeightPercent() {
        return weight.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
}