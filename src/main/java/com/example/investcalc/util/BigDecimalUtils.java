package com.example.investcalc.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Единые правила округления для денег, весов и коэффициентов.
 * Раньше {@code setScale(...)} и {@code divide(...)} с разными масштабами были разбросаны по сервисам.
 */
public final class BigDecimalUtils {

    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Деньги и проценты для отчёта. */
    public static final int MONEY_SCALE = 2;
    /** Веса, HHI. */
    public static final int WEIGHT_SCALE = 4;
    /** Коэффициенты: VaR, ES, скользящие средние. */
    public static final int RATIO_SCALE = 6;
    /** Промежуточные вычисления (доходности, нормировки). */
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

    /** Деление с округлением HALF_UP. Делитель не должен быть нулём. */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor, int scale) {
        return dividend.divide(divisor, scale, ROUNDING);
    }

    /** Деление, которое при нулевом или отсутствующем делителе возвращает ноль. */
    public static BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor, int scale) {
        if (dividend == null || isZero(divisor)) {
            return BigDecimal.ZERO.setScale(scale, ROUNDING);
        }
        return divide(dividend, divisor, scale);
    }

    /** {@code null} превращается в ноль. */
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

    /** Отрицательные значения обрезаются до нуля (VaR и ES не бывают отрицательными). */
    public static BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
