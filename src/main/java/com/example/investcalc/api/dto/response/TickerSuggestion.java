package com.example.investcalc.api.dto.response;

public record TickerSuggestion(
        String ticker,
        String name,
        String sector
) {}