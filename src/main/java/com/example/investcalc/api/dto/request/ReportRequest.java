package com.example.investcalc.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ReportRequest(

        @NotEmpty(message = "positions не могут быть пустыми")
        @Valid
        List<PositionDto> positions,

        @NotNull(message = "initialCapital обязателен")
        @DecimalMin(value = "1000.0", message = "initialCapital должен быть ≥ 1000")
        BigDecimal initialCapital,

        @NotBlank(message = "strategy обязательна")
        String strategy,

        @Min(value = 1, message = "periodYears минимум 1")
        @Max(value = 20, message = "periodYears максимум 20")
        int periodYears
) {}