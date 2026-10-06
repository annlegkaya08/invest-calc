package com.example.investcalc.exception;

import com.example.investcalc.api.exception.ApiErrorCode;

/** Внешний источник рыночных данных (MOEX ISS) недоступен или вернул ошибку. */
public class DataUnavailableException extends BusinessException {

    public DataUnavailableException(String message) {
        super(ApiErrorCode.DATA_UNAVAILABLE, message);
    }

    public DataUnavailableException(String message, Throwable cause) {
        super(ApiErrorCode.DATA_UNAVAILABLE, message, cause);
    }
}
