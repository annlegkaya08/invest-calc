package com.example.investcalc.util;

import java.math.BigDecimal;

import static com.example.investcalc.util.BigDecimalUtils.*;

/** Перевод между долями (0.05) и процентами (5.00). */
public final class PercentageUtils {

    private PercentageUtils() {
    }

    /** 0.0523 → 5.23 */
    public static BigDecimal fractionToPercent(BigDecimal fraction) {
        return toMoney(fraction.multiply(HUNDRED));
    }

    /** 5.23 → 0.0523 (точность {@link BigDecimalUtils#CALC_SCALE}) */
    public static BigDecimal percentToFraction(BigDecimal percent) {
        return divide(percent, HUNDRED, CALC_SCALE);
    }

    /** Доля {@code amount} от {@code base} в процентах; при нулевой базе вернёт 0.00. */
    public static BigDecimal ratioPercent(BigDecimal amount, BigDecimal base) {
        if (isZero(base)) {
            return toMoney(BigDecimal.ZERO);
        }
        return toMoney(divide(amount, base, CALC_SCALE).multiply(HUNDRED));
    }

    /** Изменение от {@code from} к {@code to} в процентах; при нулевой базе вернёт 0.00. */
    public static BigDecimal changePercent(BigDecimal from, BigDecimal to) {
        if (isZero(from)) {
            return toMoney(BigDecimal.ZERO);
        }
        return toMoney(divide(to.subtract(from), from, RATIO_SCALE).multiply(HUNDRED));
    }

    /** Для строк с {@code String.format}: 0.4 → 40.0 */
    public static double fractionToPercentDouble(BigDecimal fraction) {
        return fraction.multiply(HUNDRED).doubleValue();
    }

    /** 12.3 → "12.30%" */
    public static String format(BigDecimal percent) {
        return toMoney(percent).toPlainString() + "%";
    }
}
