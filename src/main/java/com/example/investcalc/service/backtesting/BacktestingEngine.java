package com.example.investcalc.service.backtesting;

import com.example.investcalc.api.dto.request.PositionDto;
import com.example.investcalc.api.dto.request.ReportRequest;
import com.example.investcalc.client.marketdata.MarketDataProvider;
import com.example.investcalc.repository.entity.Candle;
import com.example.investcalc.service.backtesting.cost.TransactionCostModel;
import com.example.investcalc.service.backtesting.model.BacktestResult;
import com.example.investcalc.service.backtesting.strategy.EquityPoint;
import com.example.investcalc.util.BigDecimalUtils;
import com.example.investcalc.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.investcalc.service.backtesting.model.Trade;
import com.example.investcalc.service.backtesting.strategy.Signal;
import com.example.investcalc.service.backtesting.strategy.StrategyFactory;
import com.example.investcalc.service.backtesting.strategy.TradingStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class BacktestingEngine {
    private static final Logger log = LoggerFactory.getLogger(BacktestingEngine.class);

    private final MarketDataProvider marketDataProvider;
    private final StrategyFactory strategyFactory;

    /** Комиссия 0.05% на сторону */
    private final TransactionCostModel costModel = TransactionCostModel.ofPercent(0.05);

    public BacktestingEngine(MarketDataProvider marketDataProvider,
                             StrategyFactory strategyFactory) {
        this.marketDataProvider = marketDataProvider;
        this.strategyFactory = strategyFactory;
    }

    public BacktestResult run(ReportRequest request) {
        TradingStrategy strategy = strategyFactory.create(request.strategy());
        LocalDate to = DateUtils.today();
        LocalDate from = DateUtils.periodStart(request.periodYears());

        BigDecimal initialCapital = request.initialCapital();

        BacktestResult result = new BacktestResult();
        result.setInitialCapital(initialCapital);

        BigDecimal cash = initialCapital;
        Map<String, BigDecimal> holdings = new HashMap<>(); // ticker -> qty
        List<Trade> allTrades = new ArrayList<>();

        Map<String, List<Candle>> candlesByTicker = new LinkedHashMap<>();
        Map<String, List<Signal>> signalsByTicker = new HashMap<>();
        Map<String, BigDecimal> weights = new HashMap<>();

        for (PositionDto pos : request.positions()) {
            String ticker = pos.ticker().toUpperCase();
            weights.put(ticker, pos.weight());
            List<Candle> candles = marketDataProvider.getCandles(ticker, from, to);
            if (candles.isEmpty()) {
                log.warn("No candles for {}, skip", ticker);
                continue;
            }
            candlesByTicker.put(ticker, candles);
            signalsByTicker.put(ticker, strategy.generateSignals(candles));
            holdings.put(ticker, BigDecimal.ZERO);
        }

        if (candlesByTicker.isEmpty()) {
            result.setFinalCapital(initialCapital);
            result.setTotalReturnPct(BigDecimal.ZERO);
            result.setTotalTrades(0);
            result.setTotalCommission(BigDecimal.ZERO);
            result.setSummaryText("Backtest failed: no market data for tickers.");
            return result;
        }

        TreeSet<LocalDate> allDates = new TreeSet<>();
        candlesByTicker.values().forEach(list -> list.forEach(c -> allDates.add(c.getDate())));

        Map<String, Integer> idx = new HashMap<>();
        candlesByTicker.keySet().forEach(t -> idx.put(t, 0));

        BigDecimal totalCommission = BigDecimal.ZERO;

        for (LocalDate date : allDates) {
            for (String ticker : candlesByTicker.keySet()) {
                List<Candle> candles = candlesByTicker.get(ticker);
                int i = idx.get(ticker);

                while (i < candles.size() && candles.get(i).getDate().isBefore(date)) {
                    i++;
                }
                idx.put(ticker, i);
                if (i >= candles.size() || !candles.get(i).getDate().equals(date)) {
                    continue;
                }

                Candle candle = candles.get(i);
                Signal signal = signalsByTicker.get(ticker).get(i);
                BigDecimal qty = holdings.get(ticker);
                BigDecimal price = candle.getClose();

                if (signal == Signal.BUY && qty.compareTo(BigDecimal.ZERO) == 0) {
                    //для тикущей эквити
                    BigDecimal equityNow = calcEquity(cash, holdings, candlesByTicker, idx);
                    BigDecimal alloc = equityNow.multiply(weights.get(ticker));
                    if (alloc.compareTo(price) > 0 && cash.compareTo(price) > 0) {
                        BigDecimal spend = alloc.min(cash);
                        BigDecimal commission = costModel.calculate(spend);
                        BigDecimal spendNet = spend.subtract(commission);
                        if (spendNet.compareTo(price) > 0) {
                            BigDecimal buyQty = spendNet.divide(price, 4, RoundingMode.DOWN);
                            if (buyQty.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal amount = buyQty.multiply(price);
                                commission = costModel.calculate(amount);
                                cash = cash.subtract(amount).subtract(commission);
                                holdings.put(ticker, buyQty);
                                totalCommission = totalCommission.add(commission);

                                Trade trade = new Trade(date, ticker, Trade.Type.BUY,
                                        price, buyQty, commission);
                                allTrades.add(trade);
                            }
                        }
                    }
                } else if (signal == Signal.SELL && qty.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal amount = qty.multiply(price);
                    BigDecimal commission = costModel.calculate(amount);
                    cash = cash.add(amount).subtract(commission);
                    holdings.put(ticker, BigDecimal.ZERO);
                    totalCommission = totalCommission.add(commission);

                    Trade trade = new Trade(date, ticker, Trade.Type.SELL,
                            price, qty, commission);
                    allTrades.add(trade);
                }
            }

            BigDecimal equity = calcEquity(cash, holdings, candlesByTicker, idx);
            result.addEquityPoint(new EquityPoint(date, BigDecimalUtils.toMoney(equity)));
        }

        int lastIdxFallback = 0;
        BigDecimal finalEquity = calcEquity(cash, holdings, candlesByTicker, idx);

        result.setFinalCapital(BigDecimalUtils.toMoney(finalEquity));
        result.setTotalTrades(allTrades.size());
        result.setTotalCommission(BigDecimalUtils.toMoney(totalCommission));
        result.getTrades().addAll(allTrades);
        result.computeReturn();
        result.setSummaryText(buildSummary(result, strategy.getName()));

        log.info("Backtest done: strategy={}, return={}%, trades={}",
                strategy.getName(), result.getTotalReturnPct(), result.getTotalTrades());

        return result;
    }


    private BigDecimal calcEquity(BigDecimal cash,
                                  Map<String, BigDecimal> holdings,
                                  Map<String, List<Candle>> candlesByTicker,
                                  Map<String, Integer> idx) {
        BigDecimal equity = cash;
        for (Map.Entry<String, BigDecimal> e : holdings.entrySet()) {
            if (e.getValue().compareTo(BigDecimal.ZERO) <= 0) continue;
            List<Candle> candles = candlesByTicker.get(e.getKey());
            int i = idx.getOrDefault(e.getKey(), 0);
            if (candles == null || candles.isEmpty()) continue;
            if (i >= candles.size()) i = candles.size() - 1;
            if (i < 0) i = 0;
            //индекс для будущей свечи
            while (i > 0 && i >= candles.size()) i--;
            BigDecimal price = candles.get(Math.min(i, candles.size() - 1)).getClose();
            equity = equity.add(e.getValue().multiply(price));
        }
        return equity;
    }

    private String buildSummary(BacktestResult r, String strategyName) {

        return """
                Strategy: %s
                Initial capital: %s
                Final capital: %s
                Total return: %s%%
                Trades: %d
                Total commission: %s
                Equity points: %d
                """.formatted(
                strategyName,
                r.getInitialCapital(),
                r.getFinalCapital(),
                r.getTotalReturnPct(),
                r.getTotalTrades(),
                r.getTotalCommission(),
                r.getEquityCurve().size()
        );
    }
}