package com.example.investcalc.service.risk;

import com.example.investcalc.api.dto.request.PositionDto;
import com.example.investcalc.api.dto.request.ReportRequest;
import com.example.investcalc.client.marketdata.MarketDataProvider;

import com.example.investcalc.repository.entity.Candle;
import com.example.investcalc.service.risk.method.HistoricalVaR;
import com.example.investcalc.service.risk.method.ParametricVaR;
import com.example.investcalc.service.risk.model.VaRResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class VaRCalculator {
    private static final Logger log = LoggerFactory.getLogger(VaRCalculator.class);

    private static final BigDecimal DEFAULT_CONFIDENCE = new BigDecimal("0.95");
    private static final int DEFAULT_HORIZON_DAYS = 1;

    private final MarketDataProvider marketDataProvider;
    private final HistoricalVaR historicalVaR;
    private final ParametricVaR parametricVaR;

    public VaRCalculator(MarketDataProvider marketDataProvider,
                         HistoricalVaR historicalVaR,
                         ParametricVaR parametricVaR) {
        this.marketDataProvider = marketDataProvider;
        this.historicalVaR = historicalVaR;
        this.parametricVaR = parametricVaR;
    }


    public VaRResult calculate(ReportRequest request) {
        return calculate(request, DEFAULT_CONFIDENCE, DEFAULT_HORIZON_DAYS);
    }

    public VaRResult calculate(ReportRequest request,
                               BigDecimal confidenceLevel,
                               int horizonDays) {

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusYears(request.periodYears());
        BigDecimal portfolioValue = request.initialCapital();

        List<BigDecimal> portfolioReturns = buildPortfolioReturns(request, from, to);

        VaRResult result = new VaRResult();
        result.setConfidenceLevel(confidenceLevel);
        result.setHorizonDays(horizonDays);
        result.setPortfolioValue(portfolioValue);
        result.setObservations(portfolioReturns.size());

        if (portfolioReturns.size() < 10) {
            result.setSummaryText("VaR: insufficient data (need ≥ 10 daily returns).");
            log.warn("VaR skipped: only {} returns", portfolioReturns.size());
            return result;
        }

        // --- Historical ---
        BigDecimal hVaRpct = historicalVaR.calculateVaR(portfolioReturns, confidenceLevel);
        BigDecimal hESpct = historicalVaR.calculateES(portfolioReturns, confidenceLevel);

        // горизонт: √t для iid (упрощение)
        if (horizonDays > 1) {
            BigDecimal scale = BigDecimal.valueOf(Math.sqrt(horizonDays));
            hVaRpct = hVaRpct.multiply(scale);
            hESpct = hESpct.multiply(scale);
        }

        result.setHistoricalVaRPct(hVaRpct.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        result.setHistoricalESPct(hESpct.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        result.setHistoricalVaR(portfolioValue.multiply(hVaRpct).setScale(2, RoundingMode.HALF_UP));
        result.setHistoricalES(portfolioValue.multiply(hESpct).setScale(2, RoundingMode.HALF_UP));

        // --- Parametric ---
        BigDecimal pVaRpct = parametricVaR.calculateVaR(portfolioReturns, confidenceLevel);
        BigDecimal pESpct = parametricVaR.calculateES(portfolioReturns, confidenceLevel);

        if (horizonDays > 1) {
            BigDecimal scale = BigDecimal.valueOf(Math.sqrt(horizonDays));
            pVaRpct = pVaRpct.multiply(scale);
            pESpct = pESpct.multiply(scale);
        }

        result.setParametricVaRPct(pVaRpct.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        result.setParametricESPct(pESpct.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        result.setParametricVaR(portfolioValue.multiply(pVaRpct).setScale(2, RoundingMode.HALF_UP));
        result.setParametricES(portfolioValue.multiply(pESpct).setScale(2, RoundingMode.HALF_UP));

        result.setSummaryText(buildSummary(result));
        log.info("VaR done: hist={} ({}%), param={} ({}%), n={}",
                result.getHistoricalVaR(), result.getHistoricalVaRPct(),
                result.getParametricVaR(), result.getParametricVaRPct(),
                result.getObservations());

        return result;
    }

    /**
     * Дневные доходности портфеля по весам.
     * r_p,t = Σ w_i * r_i,t
     */
    private List<BigDecimal> buildPortfolioReturns(ReportRequest request,
                                                   LocalDate from,
                                                   LocalDate to) {

        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        Map<String, List<Candle>> candlesMap = new LinkedHashMap<>();

        for (PositionDto pos : request.positions()) {
            String ticker = pos.ticker().toUpperCase();
            weights.put(ticker, pos.weight());
            List<Candle> candles = marketDataProvider.getCandles(ticker, from, to);
            if (!candles.isEmpty()) {
                candlesMap.put(ticker, candles);
            }
        }

        if (candlesMap.isEmpty()) {
            return List.of();
        }

        // индекс дата → доходность тикера
        Map<String, Map<LocalDate, BigDecimal>> returnsByTicker = new HashMap<>();
        TreeSet<LocalDate> allDates = new TreeSet<>();

        for (Map.Entry<String, List<Candle>> e : candlesMap.entrySet()) {
            List<Candle> candles = e.getValue();
            Map<LocalDate, BigDecimal> ret = new HashMap<>();
            for (int i = 1; i < candles.size(); i++) {
                BigDecimal prev = candles.get(i - 1).getClose();
                BigDecimal curr = candles.get(i).getClose();
                if (prev.compareTo(BigDecimal.ZERO) == 0) continue;
                BigDecimal r = curr.subtract(prev)
                        .divide(prev, 8, RoundingMode.HALF_UP);
                LocalDate d = candles.get(i).getDate();
                ret.put(d, r);
                allDates.add(d);
            }
            returnsByTicker.put(e.getKey(), ret);
        }

        List<BigDecimal> portfolioReturns = new ArrayList<>();
        for (LocalDate date : allDates) {
            BigDecimal rp = BigDecimal.ZERO;
            BigDecimal weightUsed = BigDecimal.ZERO;

            for (Map.Entry<String, BigDecimal> w : weights.entrySet()) {
                Map<LocalDate, BigDecimal> ret = returnsByTicker.get(w.getKey());
                if (ret == null) continue;
                BigDecimal r = ret.get(date);
                if (r == null) continue;
                rp = rp.add(w.getValue().multiply(r));
                weightUsed = weightUsed.add(w.getValue());
            }

            // нормализуем, если в этот день не все тикеры торговались
            if (weightUsed.compareTo(BigDecimal.ZERO) > 0
                    && weightUsed.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.01")) > 0) {
                rp = rp.divide(weightUsed, 8, RoundingMode.HALF_UP);
            }

            if (weightUsed.compareTo(BigDecimal.ZERO) > 0) {
                portfolioReturns.add(rp);
            }
        }

        return portfolioReturns;
    }

    private String buildSummary(VaRResult r) {
        return """
                Confidence: %s | Horizon: %d day(s) | Observations: %d
                Portfolio value: %s

                Historical VaR:  %s  (%s%%)
                Historical ES:   %s  (%s%%)

                Parametric VaR:  %s  (%s%%)
                Parametric ES:   %s  (%s%%)
                """.formatted(
                r.getConfidenceLevel(),
                r.getHorizonDays(),
                r.getObservations(),
                r.getPortfolioValue(),
                r.getHistoricalVaR(), r.getHistoricalVaRPct(),
                r.getHistoricalES(), r.getHistoricalESPct(),
                r.getParametricVaR(), r.getParametricVaRPct(),
                r.getParametricES(), r.getParametricESPct()
        );
    }
}