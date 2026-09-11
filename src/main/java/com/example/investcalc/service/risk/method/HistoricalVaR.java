package com.example.investcalc.service.risk.method;

import org.springframework.stereotype.Component;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Component
public class HistoricalVaR implements VaRMethod {
    @Override
    public BigDecimal calculateVaR(List<BigDecimal> returns, BigDecimal confidenceLevel) {
        if (returns == null || returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> sorted = new ArrayList<>(returns);
        Collections.sort(sorted);

        double alpha = 1.0 - confidenceLevel.doubleValue();
        int index = (int) Math.floor(alpha * sorted.size());
        index = Math.max(0, Math.min(index, sorted.size() - 1));

        BigDecimal quantile = sorted.get(index);
        return quantile.negate().max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
    }


    @Override
    public BigDecimal calculateES(List<BigDecimal> returns, BigDecimal confidenceLevel) {
        if (returns == null || returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> sorted = new ArrayList<>(returns);
        Collections.sort(sorted);

        double alpha = 1.0 - confidenceLevel.doubleValue();
        int count = Math.max(1, (int) Math.floor(alpha * sorted.size()));

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < count; i++) {
            sum = sum.add(sorted.get(i));
        }
        BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP);
        return avg.negate().max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
    }
}