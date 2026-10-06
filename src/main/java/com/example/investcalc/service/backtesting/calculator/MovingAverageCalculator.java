package com.example.investcalc.service.backtesting.calculator;

import com.example.investcalc.util.BigDecimalUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class MovingAverageCalculator {

    private MovingAverageCalculator() {}

    //Скользящая средняя
    public static List<BigDecimal> sma(List<BigDecimal> prices, int period) {
        List<BigDecimal> result = new ArrayList<>(prices.size());
        if (period <= 0 || prices.isEmpty()) {
            return result;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < prices.size(); i++) {
            sum = sum.add(prices.get(i));
            if (i >= period) {
                sum = sum.subtract(prices.get(i - period));
            }
            if (i >= period - 1) {
                result.add(BigDecimalUtils.divide(sum, BigDecimal.valueOf(period), BigDecimalUtils.RATIO_SCALE));
            } else {
                result.add(null);
            }
        }
        return result;
    }
}