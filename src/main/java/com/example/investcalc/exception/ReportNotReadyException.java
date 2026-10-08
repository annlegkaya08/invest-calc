package com.example.investcalc.exception;

import com.example.investcalc.api.exception.ApiErrorCode;

/** Отчёт ещё считается или завершился ошибкой, скачать нечего. */
public class ReportNotReadyException extends BusinessException {

    public ReportNotReadyException(String reportId, String status) {
        super(ApiErrorCode.REPORT_NOT_READY,
                "Отчёт " + reportId + " недоступен для скачивания, статус: " + status);
    }
}
