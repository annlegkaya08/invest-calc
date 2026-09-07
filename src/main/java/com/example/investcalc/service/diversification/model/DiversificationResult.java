package com.example.investcalc.service.diversification.model;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DiversificationResult {

    private final List<SectorAllocation> sectorAllocations = new ArrayList<>();
    private final List<RebalanceRecommendation> recommendations = new ArrayList<>();
    private BigDecimal hhi;
    private String concentrationLevel;
    private String summaryText;

    public List<SectorAllocation> getSectorAllocations() {
        return sectorAllocations;
    }

    public List<RebalanceRecommendation> getRecommendations() {
        return recommendations;
    }

    public BigDecimal getHhi() {
        return hhi;
    }

    public void setHhi(BigDecimal hhi) {
        this.hhi = hhi;
    }

    public String getConcentrationLevel() {
        return concentrationLevel;
    }

    public void setConcentrationLevel(String concentrationLevel) {
        this.concentrationLevel = concentrationLevel;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public void addAllocation(SectorAllocation allocation) {
        sectorAllocations.add(allocation);
    }

    public void addRecommendation(RebalanceRecommendation recommendation) {
        recommendations.add(recommendation);
    }
}