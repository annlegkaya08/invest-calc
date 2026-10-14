package com.example.investcalc.service.diversification.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RebalanceRecommendation {

    public enum Severity {
        INFO, WARNING, CRITICAL
    }

    private final Severity severity;
    private final String message;
    private final String sector;
    private final BigDecimal currentWeight;
    private final BigDecimal suggestedMaxWeight;
    private final Map<String, BigDecimal> suggestedWeights;

    public RebalanceRecommendation(Severity severity,
                                   String message,
                                   String sector,
                                   BigDecimal currentWeight,
                                   BigDecimal suggestedMaxWeight,
                                   Map<String, BigDecimal> suggestedWeights) {
        this.severity = severity;
        this.message = message;
        this.sector = sector;
        this.currentWeight = currentWeight;
        this.suggestedMaxWeight = suggestedMaxWeight;
        this.suggestedWeights = suggestedWeights == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(suggestedWeights));
    }

    public static RebalanceRecommendation info(String message) {
        return new RebalanceRecommendation(Severity.INFO, message, null, null, null, null);
    }

    public static RebalanceRecommendation warning(String message, String sector,
                                                  BigDecimal current, BigDecimal max) {
        return new RebalanceRecommendation(Severity.WARNING, message, sector, current, max, null);
    }

    public static RebalanceRecommendation critical(String message, String sector,
                                                   BigDecimal current, BigDecimal max) {
        return new RebalanceRecommendation(Severity.CRITICAL, message, sector, current, max, null);
    }

    public static RebalanceRecommendation withSuggestedWeights(
            Severity severity,
            String message,
            String sector,
            BigDecimal currentWeight,
            BigDecimal suggestedMaxWeight,
            Map<String, BigDecimal> suggestedWeights) {
        return new RebalanceRecommendation(
                severity, message, sector, currentWeight, suggestedMaxWeight, suggestedWeights);
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

    public Map<String, BigDecimal> getSuggestedWeights() {
        return suggestedWeights;
    }

    public boolean hasSuggestedWeights() {
        return suggestedWeights != null && !suggestedWeights.isEmpty();
    }
}
