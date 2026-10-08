package com.example.investcalc.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

public final class BigDecimalUtils {

    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static final int MONEY_SCALE = 2;
    public static final int WEIGHT_SCALE = 4;
    public static final int RATIO_SCALE = 6;
    public static final int CALC_SCALE = 8;

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private BigDecimalUtils() {
    }

    public static BigDecimal round(BigDecimal value, int scale) {
        return value.setScale(scale, ROUNDING);
    }

    public static BigDecimal toMoney(BigDecimal value) {
        return round(value, MONEY_SCALE);
    }

    public static BigDecimal toWeight(BigDecimal value) {
        return round(value, WEIGHT_SCALE);
    }

    public static BigDecimal toRatio(BigDecimal value) {
        return round(value, RATIO_SCALE);
    }
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor, int scale) {
        return dividend.divide(divisor, scale, ROUNDING);
    }

    public static BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor, int scale) {
        if (dividend == null || isZero(divisor)) {
            return BigDecimal.ZERO.setScale(scale, ROUNDING);
        }
        return divide(dividend, divisor, scale);
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static boolean isZero(BigDecimal value) {
        return value == null || value.signum() == 0;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static BigDecimal sum(Collection<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            total = total.add(nz(v));
        }
        return total;
    }

    public static BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
