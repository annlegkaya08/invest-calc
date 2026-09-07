package com.example.investcalc.service.diversification.model;

import java.math.BigDecimal;

public class RebalanceRecommendation {

    public enum Severity {
        INFO, WARNING, CRITICAL
    }

    private final Severity severity;
    private final String message;
    private final String sector;
    private final BigDecimal currentWeight;
    private final BigDecimal suggestedMaxWeight;

    public RebalanceRecommendation(Severity severity,
                                   String message,
                                   String sector,
                                   BigDecimal currentWeight,
                                   BigDecimal suggestedMaxWeight) {
        this.severity = severity;
        this.message = message;
        this.sector = sector;
        this.currentWeight = currentWeight;
        this.suggestedMaxWeight = suggestedMaxWeight;
    }

    public static RebalanceRecommendation info(String message) {
        return new RebalanceRecommendation(Severity.INFO, message, null, null, null);
    }

    public static RebalanceRecommendation warning(String message, String sector,
                                                  BigDecimal current, BigDecimal max) {
        return new RebalanceRecommendation(Severity.WARNING, message, sector, current, max);
    }

    public static RebalanceRecommendation critical(String message, String sector,
                                                   BigDecimal current, BigDecimal max) {
        return new RebalanceRecommendation(Severity.CRITICAL, message, sector, current, max);
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getSector() {
        return sector;
    }

    public BigDecimal getCurrentWeight() {
        return currentWeight;
    }

    public BigDecimal getSuggestedMaxWeight() {
        return suggestedMaxWeight;
    }
}