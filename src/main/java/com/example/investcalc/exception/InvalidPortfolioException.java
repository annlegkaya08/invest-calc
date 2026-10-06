package com.example.investcalc.exception;

import com.example.investcalc.api.exception.ApiErrorCode;

import java.util.List;

public class InvalidPortfolioException extends BusinessException {

    public InvalidPortfolioException(String message) {
        super(ApiErrorCode.INVALID_PORTFOLIO, message);
    }

    public InvalidPortfolioException(String message, List<String> details) {
        super(ApiErrorCode.INVALID_PORTFOLIO, message, details);
    }
}
