package com.example.investcalc.service.backtesting.cost;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TransactionCostModel {
    private final BigDecimal commissionRate;

    public TransactionCostModel(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public static TransactionCostModel ofPercent(double percent) {
        return new TransactionCostModel(
                BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
        );
    }

    public BigDecimal calculate(BigDecimal tradeAmount) {
        return tradeAmount.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
    }
    public BigDecimal getCommissionRate() {
        return commissionRate;
    }
}