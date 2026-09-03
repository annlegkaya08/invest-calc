package com.example.investcalc.api.dto.response;

import java.time.Instant;

public record ReportResponse(
        String reportId,
        String status,
        String downloadUrl,     //заполняется когда status = COMPLETED
        Instant createdAt
) {}