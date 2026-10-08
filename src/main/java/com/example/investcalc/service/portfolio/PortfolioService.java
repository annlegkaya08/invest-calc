package com.example.investcalc.service.portfolio;

import com.example.investcalc.api.dto.request.PositionDto;
import com.example.investcalc.api.dto.request.ReportRequest;
import com.example.investcalc.service.diversification.model.DiversificationResult;
import com.example.investcalc.service.diversification.model.RebalanceRecommendation;
import com.example.investcalc.service.portfolio.model.Portfolio;
import com.example.investcalc.service.portfolio.model.WeightedPosition;
import com.example.investcalc.util.BigDecimalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    public Portfolio fromRequest(ReportRequest request) {
        Portfolio portfolio = new Portfolio(request.initialCapital());
        for (PositionDto dto : request.positions()) {
            portfolio.addPosition(new WeightedPosition(dto.ticker(), dto.weight()));
        }
        return portfolio;
    }

    public Portfolio applyRecommendations(Portfolio original, DiversificationResult diversification) {
        if (original == null) {
            throw new IllegalArgumentException("portfolio must not be null");
        }
        Map<String, BigDecimal> suggested = resolveSuggestedWeights(diversification);
        if (suggested.isEmpty()) {
            log.info("No suggested weights  returning original portfolio");
            return copy(original);
        }
        return applyWeights(original, suggested);
    }

    public Portfolio applyRecommendations(Portfolio original, Map<String, BigDecimal> suggestedWeights) {
        if (original == null) {
            throw new IllegalArgumentException("portfolio must not be null");
        }
        if (suggestedWeights == null || suggestedWeights.isEmpty()) {
            return copy(original);
        }
        return applyWeights(original, suggestedWeights);
    }

    public Portfolio applyRecommendations(Portfolio original, List<RebalanceRecommendation> recommendations) {
        if (original == null) {
            throw new IllegalArgumentException("portfolio must not be null");
        }
        Map<String, BigDecimal> suggested = Map.of();
        if (recommendations != null) {
            for (RebalanceRecommendation r : recommendations) {
                if (r != null && r.hasSuggestedWeights()) {
                    suggested = r.getSuggestedWeights();
                }
            }
        }
        if (suggested.isEmpty()) {
            return copy(original);
        }
        return applyWeights(original, suggested);
    }

    private Map<String, BigDecimal> resolveSuggestedWeights(DiversificationResult result) {
        if (result == null) {
            return Map.of();
        }
        if (result.getSuggestedWeights() != null && !result.getSuggestedWeights().isEmpty()) {
            return result.getSuggestedWeights();
        }
        Map<String, BigDecimal> fromRec = Map.of();
        for (RebalanceRecommendation r : result.getRecommendations()) {
            if (r.hasSuggestedWeights()) {
                fromRec = r.getSuggestedWeights();
            }
        }
        return fromRec;
    }

    private Portfolio applyWeights(Portfolio original, Map<String, BigDecimal> suggestedWeights) {
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        for (WeightedPosition p : original.getPositions()) {
            BigDecimal w = suggestedWeights.getOrDefault(p.getTicker(), BigDecimal.ZERO);
            if (w.signum() < 0) {
                w = BigDecimal.ZERO;
            }
            weights.put(p.getTicker(), w);
        }

        BigDecimal sum = BigDecimalUtils.sum(weights.values());
        Portfolio adjusted = new Portfolio(original.getInitialCapital());

        if (sum.signum() <= 0) {
            BigDecimal eq = BigDecimalUtils.toWeight(
                    BigDecimalUtils.safeDivide(
                            BigDecimal.ONE,
                            BigDecimal.valueOf(Math.max(1, weights.size())),
                            BigDecimalUtils.CALC_SCALE));
            for (WeightedPosition p : original.getPositions()) {
                WeightedPosition np = new WeightedPosition(p.getTicker(), eq);
                np.setSector(p.getSector());
                adjusted.addPosition(np);
            }
            log.info("applyRecommendations: zero suggested sum � equal weights, n={}", weights.size());
            return adjusted;
        }

        for (WeightedPosition p : original.getPositions()) {
            BigDecimal raw = weights.get(p.getTicker());
            BigDecimal normalized = BigDecimalUtils.toWeight(
                    BigDecimalUtils.safeDivide(raw, sum, BigDecimalUtils.CALC_SCALE));
            WeightedPosition np = new WeightedPosition(p.getTicker(), normalized);
            np.setSector(p.getSector());
            adjusted.addPosition(np);
        }

        BigDecimal total = adjusted.totalWeight();
        if (total.compareTo(BigDecimal.ONE) != 0 && !adjusted.getPositions().isEmpty()) {
            List<WeightedPosition> list = adjusted.getPositions();
            WeightedPosition last = list.get(list.size() - 1);
            BigDecimal diff = BigDecimal.ONE.subtract(total);
            BigDecimal fixed = BigDecimalUtils.toWeight(last.getWeight().add(diff));
            Portfolio fixedPortfolio = new Portfolio(original.getInitialCapital());
            for (int i = 0; i < list.size() - 1; i++) {
                fixedPortfolio.addPosition(list.get(i));
            }
            WeightedPosition lastFixed = new WeightedPosition(last.getTicker(), fixed);
            lastFixed.setSector(last.getSector());
            fixedPortfolio.addPosition(lastFixed);
            adjusted = fixedPortfolio;
        }

        log.info("applyRecommendations: adjusted portfolio totalWeight={}, positions={}",
                adjusted.totalWeight(), adjusted.getPositions().size());
        return adjusted;
    }

    private Portfolio copy(Portfolio original) {
        Portfolio copy = new Portfolio(original.getInitialCapital());
        for (WeightedPosition p : original.getPositions()) {
            WeightedPosition np = new WeightedPosition(p.getTicker(), p.getWeight());
            np.setSector(p.getSector());
            copy.addPosition(np);
        }
        return copy;
    }
}
