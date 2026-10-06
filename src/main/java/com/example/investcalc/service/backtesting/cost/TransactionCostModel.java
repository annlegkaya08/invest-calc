package com.example.investcalc.service.backtesting.cost;

import com.example.investcalc.util.BigDecimalUtils;
import com.example.investcalc.util.PercentageUtils;

import java.math.BigDecimal;

public class TransactionCostModel {
    private final BigDecimal commissionRate;

    public TransactionCostModel(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public static TransactionCostModel ofPercent(double percent) {
        return new TransactionCostModel(PercentageUtils.percentToFraction(BigDecimal.valueOf(percent)));
    }

    public BigDecimal calculate(BigDecimal tradeAmount) {
        return BigDecimalUtils.toMoney(tradeAmount.multiply(commissionRate));
    }
    public BigDecimal getCommissionRate() {
        return commissionRate;
    }
}