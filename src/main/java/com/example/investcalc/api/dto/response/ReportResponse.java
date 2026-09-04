package com.example.investcalc.api.dto.response;

import java.time.Instant;

public record ReportResponse(
        String reportId,
        String status,
        String downloadUrl,
        Instant createdAt,
        String errorMessage
) {
    public ReportResponse(String reportId, String status, String downloadUrl, Instant createdAt) {
        this(reportId, status, downloadUrl, createdAt, null);
    }
}