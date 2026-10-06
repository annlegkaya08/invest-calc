package com.example.investcalc.service.backtesting.model;

import com.example.investcalc.service.backtesting.strategy.EquityPoint;
import com.example.investcalc.util.PercentageUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BacktestResult {

    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    private BigDecimal totalReturnPct;
    private int totalTrades;
    private BigDecimal totalCommission;
    private final List<Trade> trades = new ArrayList<>();
    private final List<EquityPoint> equityCurve = new ArrayList<>();
    private String summaryText;

    public BigDecimal getInitialCapital() { return initialCapital; }
    public void setInitialCapital(BigDecimal v) { this.initialCapital = v; }

    public BigDecimal getFinalCapital() { return finalCapital; }
    public void setFinalCapital(BigDecimal v) { this.finalCapital = v; }

    public BigDecimal getTotalReturnPct() { return totalReturnPct; }
    public void setTotalReturnPct(BigDecimal v) { this.totalReturnPct = v; }

    public int getTotalTrades() { return totalTrades; }
    public void setTotalTrades(int v) { this.totalTrades = v; }

    public BigDecimal getTotalCommission() { return totalCommission; }
    public void setTotalCommission(BigDecimal v) { this.totalCommission = v; }

    public List<Trade> getTrades() { return trades; }
    public List<EquityPoint> getEquityCurve() { return equityCurve; }

    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String v) { this.summaryText = v; }

    public void addTrade(Trade t) { trades.add(t); }
    public void addEquityPoint(EquityPoint p) { equityCurve.add(p); }

    public void computeReturn() {
        if (initialCapital != null && finalCapital != null
                && initialCapital.compareTo(BigDecimal.ZERO) > 0) {
            totalReturnPct = PercentageUtils.changePercent(initialCapital, finalCapital);
        }
    }
}