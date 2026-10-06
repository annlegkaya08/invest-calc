package com.example.investcalc.service.risk.model;

import java.math.BigDecimal;

public class VaRResult {

    private BigDecimal confidenceLevel;
    private int horizonDays;

    // Historical
    private BigDecimal historicalVaR;
    private BigDecimal historicalVaRPct;
    //Expected Shortfall
    private BigDecimal historicalES;
    private BigDecimal historicalESPct;


    private BigDecimal parametricVaR;
    private BigDecimal parametricVaRPct;
    private BigDecimal parametricES;
    private BigDecimal parametricESPct;



    private BigDecimal portfolioValue;
    private int observations;
    private String summaryText;

    public BigDecimal getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(BigDecimal v) { this.confidenceLevel = v; }

    public int getHorizonDays() { return horizonDays; }
    public void setHorizonDays(int v) { this.horizonDays = v; }

    public BigDecimal getHistoricalVaR() { return historicalVaR; }
    public void setHistoricalVaR(BigDecimal v) { this.historicalVaR = v; }

    public BigDecimal getHistoricalVaRPct() { return historicalVaRPct; }
    public void setHistoricalVaRPct(BigDecimal v) { this.historicalVaRPct = v; }

    public BigDecimal getHistoricalES() { return historicalES; }
    public void setHistoricalES(BigDecimal v) { this.historicalES = v; }

    public BigDecimal getHistoricalESPct() { return historicalESPct; }
    public void setHistoricalESPct(BigDecimal v) { this.historicalESPct = v; }

    public BigDecimal getParametricVaR() { return parametricVaR; }
    public void setParametricVaR(BigDecimal v) { this.parametricVaR = v; }

    public BigDecimal getParametricVaRPct() { return parametricVaRPct; }
    public void setParametricVaRPct(BigDecimal v) { this.parametricVaRPct = v; }

    public BigDecimal getParametricES() { return parametricES; }
    public void setParametricES(BigDecimal v) { this.parametricES = v; }

    public BigDecimal getParametricESPct() { return parametricESPct; }
    public void setParametricESPct(BigDecimal v) { this.parametricESPct = v; }

    public BigDecimal getPortfolioValue() { return portfolioValue; }
    public void setPortfolioValue(BigDecimal v) { this.portfolioValue = v; }

    public int getObservations() { return observations; }
    public void setObservations(int v) { this.observations = v; }

    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String v) { this.summaryText = v; }
}
