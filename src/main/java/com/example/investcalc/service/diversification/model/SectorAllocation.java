package com.example.investcalc.service.diversification.model;

import com.example.investcalc.util.BigDecimalUtils;
import com.example.investcalc.util.PercentageUtils;

import java.math.BigDecimal;

public class SectorAllocation {

    private final String sector;
    private final BigDecimal weight;
    private final int positionCount;

    public SectorAllocation(String sector, BigDecimal weight, int positionCount) {
        this.sector = sector;
        this.weight = BigDecimalUtils.toWeight(weight);
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
        return PercentageUtils.fractionToPercent(weight);
    }
}