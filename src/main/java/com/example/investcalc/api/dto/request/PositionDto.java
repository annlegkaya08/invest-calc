package com.example.investcalc.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PositionDto(
        @NotBlank(message = "ticker обязателен")
        String ticker,

        @NotNull(message = "weight обязателен")
        @DecimalMin(value = "0.0", inclusive = false, message = "weight должен быть > 0")
        @DecimalMax(value = "1.0", message = "weight не может быть > 1")
        BigDecimal weight
) {}