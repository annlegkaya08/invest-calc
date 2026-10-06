package com.example.investcalc.exception;

import com.example.investcalc.api.exception.ApiErrorCode;

public class ReportGenerationException extends BusinessException {

    public ReportGenerationException(String message) {
        super(ApiErrorCode.REPORT_GENERATION_FAILED, message);
    }

    public ReportGenerationException(String message, Throwable cause) {
        super(ApiErrorCode.REPORT_GENERATION_FAILED, message, cause);
    }
}
