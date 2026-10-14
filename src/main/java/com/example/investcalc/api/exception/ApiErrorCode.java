package com.example.investcalc.api.exception;

//Машиночитаемые коды ошибок API
public enum ApiErrorCode {

    VALIDATION_ERROR(400),
    MALFORMED_REQUEST(400),
    UNKNOWN_STRATEGY(400),
    INVALID_PORTFOLIO(422),
    INSUFFICIENT_DATA(422),
    TICKER_NOT_FOUND(404),
    REPORT_NOT_FOUND(404),
    REPORT_NOT_READY(409),
    DATA_UNAVAILABLE(503),
    REPORT_GENERATION_FAILED(500),
    INTERNAL_ERROR(500);

    private final int httpStatus;

    ApiErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
