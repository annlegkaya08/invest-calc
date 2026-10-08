package com.example.investcalc.exception;

import com.example.investcalc.api.exception.ApiErrorCode;

public class ReportNotFoundException extends BusinessException {

    public ReportNotFoundException(String reportId) {
        super(ApiErrorCode.REPORT_NOT_FOUND, "Отчёт не найден: " + reportId);
    }
}
