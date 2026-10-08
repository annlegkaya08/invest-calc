package com.example.investcalc.service.diversification.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DiversificationResult {

    private final List<SectorAllocation> sectorAllocations = new ArrayList<>();
    private final List<RebalanceRecommendation> recommendations = new ArrayList<>();
    private BigDecimal hhi;
    private BigDecimal effectiveSectorCount;
    private BigDecimal top1Share;
    private BigDecimal top3Share;
    private String concentrationLevel;
    private String summaryText;
    /** Aggregate suggested ticker weights after applying rebalance logic (sum ≈ 1). */
    private Map<String, BigDecimal> suggestedWeights = new LinkedHashMap<>();

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

    public BigDecimal getEffectiveSectorCount() {
        return effectiveSectorCount;
    }

    public void setEffectiveSectorCount(BigDecimal effectiveSectorCount) {
        this.effectiveSectorCount = effectiveSectorCount;
    }

    public BigDecimal getTop1Share() {
        return top1Share;
    }

    public void setTop1Share(BigDecimal top1Share) {
        this.top1Share = top1Share;
    }

    public BigDecimal getTop3Share() {
        return top3Share;
    }

    public void setTop3Share(BigDecimal top3Share) {
        this.top3Share = top3Share;
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

    public Map<String, BigDecimal> getSuggestedWeights() {
        return suggestedWeights;
    }

    public void setSuggestedWeights(Map<String, BigDecimal> suggestedWeights) {
        this.suggestedWeights = suggestedWeights != null
                ? new LinkedHashMap<>(suggestedWeights)
                : new LinkedHashMap<>();
    }

    public void addAllocation(SectorAllocation allocation) {
        sectorAllocations.add(allocation);
    }

    public void addRecommendation(RebalanceRecommendation recommendation) {
        recommendations.add(recommendation);
    }
}
