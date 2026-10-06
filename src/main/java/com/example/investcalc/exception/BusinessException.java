package com.example.investcalc.exception;

import com.example.investcalc.api.exception.ApiErrorCode;

import java.util.List;

/**
 * Базовое исключение бизнес-логики. Несёт код ошибки API и (опционально) список деталей.
 * Все предсказуемые ошибки приложения наследуются от него, а {@code GlobalExceptionHandler}
 * превращает их в единый {@code ErrorResponse}.
 */
public class BusinessException extends RuntimeException {

    private final ApiErrorCode errorCode;
    private final List<String> details;

    public BusinessException(ApiErrorCode errorCode, String message) {
        this(errorCode, message, List.of(), null);
    }

    public BusinessException(ApiErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, List.of(), cause);
    }

    public BusinessException(ApiErrorCode errorCode, String message, List<String> details) {
        this(errorCode, message, details, null);
    }

    public BusinessException(ApiErrorCode errorCode, String message, List<String> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public List<String> getDetails() {
        return details;
    }
}
