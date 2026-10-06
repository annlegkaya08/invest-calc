package com.example.investcalc.exception;

import com.example.investcalc.api.exception.ApiErrorCode;

/** Данных недостаточно для расчёта (например, меньше 10 дневных доходностей для VaR). */
public class InsufficientDataException extends BusinessException {

    public InsufficientDataException(String what, int required, int actual) {
        super(ApiErrorCode.INSUFFICIENT_DATA,
                String.format("Недостаточно данных для расчёта: %s (нужно ≥ %d, есть %d)", what, required, actual));
    }
}
