package com.example.investcalc.service.diversification.calculator;

import com.example.investcalc.api.dto.request.PositionDto;
import com.example.investcalc.api.dto.request.ReportRequest;
import com.example.investcalc.service.diversification.SectorMappingService;
import com.example.investcalc.service.diversification.model.*;
import com.example.investcalc.service.portfolio.model.Portfolio;
import com.example.investcalc.service.portfolio.model.WeightedPosition;
import com.example.investcalc.util.BigDecimalUtils;
import com.example.investcalc.util.PercentageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(PortfolioAnalyzer.class);

    private static final BigDecimal MAX_SECTOR_WEIGHT = new BigDecimal("0.40");
    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    private final SectorMappingService sectorMappingService;
    private final ConcentrationCalculator concentrationCalculator;

    public PortfolioAnalyzer(SectorMappingService sectorMappingService,
                             ConcentrationCalculator concentrationCalculator) {
        this.sectorMappingService = sectorMappingService;
        this.concentrationCalculator = concentrationCalculator;
    }

    public DiversificationResult analyze(ReportRequest request) {
        Portfolio portfolio = buildPortfolio(request);
        enrichWithSectors(portfolio);

        DiversificationResult result = new DiversificationResult();

        Map<String, BigDecimal> sectorWeights = calcSectorWeights(portfolio);
        Map<String, Long> sectorCounts = portfolio.getPositions().stream()
                .collect(Collectors.groupingBy(
                        p -> normalizeSector(p.getSector()),
                        Collectors.counting()
                ));

        sectorWeights.forEach((sector, weight) -> {
            int count = sectorCounts.getOrDefault(sector, 0L).intValue();
            result.addAllocation(new SectorAllocation(sector, weight, count));
        });

        result.getSectorAllocations().sort(
                Comparator.comparing(SectorAllocation::getWeight).reversed()
        );

        BigDecimal hhi = concentrationCalculator.hhi(sectorWeights);
        result.setHhi(hhi);
        result.setEffectiveSectorCount(concentrationCalculator.effectiveNumberOfSectors(hhi));
        result.setTop1Share(concentrationCalculator.topNShare(sectorWeights, 1));
        result.setTop3Share(concentrationCalculator.topNShare(sectorWeights, 3));
        result.setConcentrationLevel(concentrationCalculator.concentrationLevel(hhi));

        Map<String, BigDecimal> suggested = computeSuggestedWeights(portfolio, sectorWeights);
        result.setSuggestedWeights(suggested);

        buildRecommendations(result, sectorWeights, hhi, portfolio, suggested);
        result.setSummaryText(buildSummaryText(result));

        log.info("Diversification done: HHI={}, effectiveN={}, level={}, sectors={}",
                hhi, result.getEffectiveSectorCount(),
                result.getConcentrationLevel(), sectorWeights.size());

        return result;
    }

    private Portfolio buildPortfolio(ReportRequest request) {
        Portfolio portfolio = new Portfolio(request.initialCapital());
        for (PositionDto dto : request.positions()) {
            portfolio.addPosition(new WeightedPosition(dto.ticker(), dto.weight()));
        }
        return portfolio;
    }

    private void enrichWithSectors(Portfolio portfolio) {
        for (WeightedPosition pos : portfolio.getPositions()) {
            String raw = sectorMappingService.resolveSector(pos.getTicker());
            String sector = Sector.fromCode(raw).code();
            pos.setSector(sector);
            log.info("Ticker {} → sector {}", pos.getTicker(), sector);
        }
    }

    private static String normalizeSector(String sector) {
        return Sector.fromCode(sector).code();
    }

    private Map<String, BigDecimal> calcSectorWeights(Portfolio portfolio) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (WeightedPosition pos : portfolio.getPositions()) {
            String sector = normalizeSector(pos.getSector());
            map.merge(sector, pos.getWeight(), BigDecimal::add);
        }
        return map;
    }

    Map<String, BigDecimal> computeSuggestedWeights(Portfolio portfolio,
                                                    Map<String, BigDecimal> sectorWeights) {
        List<WeightedPosition> positions = portfolio.getPositions();
        if (positions.isEmpty()) {
            return Map.of();
        }

        // Start from current ticker weights
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        Map<String, String> tickerSector = new LinkedHashMap<>();
        for (WeightedPosition p : positions) {
            weights.put(p.getTicker(), p.getWeight());
            tickerSector.put(p.getTicker(), normalizeSector(p.getSector()));
        }

        BigDecimal excess = BigDecimal.ZERO;
        Set<String> cappedSectors = new HashSet<>();

        for (Map.Entry<String, BigDecimal> e : sectorWeights.entrySet()) {
            if (e.getValue().compareTo(MAX_SECTOR_WEIGHT) > 0) {
                cappedSectors.add(e.getKey());
                BigDecimal sectorW = e.getValue();
                BigDecimal scale = BigDecimalUtils.safeDivide(MAX_SECTOR_WEIGHT, sectorW, BigDecimalUtils.CALC_SCALE);
                BigDecimal reduced = BigDecimal.ZERO;
                for (Map.Entry<String, BigDecimal> tw : weights.entrySet()) {
                    if (e.getKey().equals(tickerSector.get(tw.getKey()))) {
                        BigDecimal newW = tw.getValue().multiply(scale, MC);
                        excess = excess.add(tw.getValue().subtract(newW), MC);
                        weights.put(tw.getKey(), newW);
                        reduced = reduced.add(newW, MC);
                    }
                }
            }
        }

        if (excess.signum() > 0) {
            BigDecimal freeTotal = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> tw : weights.entrySet()) {
                if (!cappedSectors.contains(tickerSector.get(tw.getKey()))) {
                    freeTotal = freeTotal.add(tw.getValue(), MC);
                }
            }
            if (freeTotal.signum() > 0) {
                for (Map.Entry<String, BigDecimal> tw : weights.entrySet()) {
                    if (!cappedSectors.contains(tickerSector.get(tw.getKey()))) {
                        BigDecimal share = BigDecimalUtils.safeDivide(tw.getValue(), freeTotal, BigDecimalUtils.CALC_SCALE);
                        weights.put(tw.getKey(), tw.getValue().add(excess.multiply(share, MC), MC));
                    }
                }
            } else {
                BigDecimal perTicker = BigDecimalUtils.safeDivide(
                        excess, BigDecimal.valueOf(weights.size()), BigDecimalUtils.CALC_SCALE);
                weights.replaceAll((t, w) -> w.add(perTicker, MC));
            }
        }

        BigDecimal sum = BigDecimalUtils.sum(weights.values());
        if (sum.signum() > 0) {
            Map<String, BigDecimal> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, BigDecimal> e : weights.entrySet()) {
                normalized.put(e.getKey(),
                        BigDecimalUtils.toWeight(BigDecimalUtils.safeDivide(e.getValue(), sum, BigDecimalUtils.CALC_SCALE)));
            }
            BigDecimal normSum = BigDecimalUtils.sum(normalized.values());
            if (normSum.compareTo(BigDecimal.ONE) != 0 && !normalized.isEmpty()) {
                String last = normalized.keySet().stream().reduce((a, b) -> b).orElse(null);
                if (last != null) {
                    BigDecimal diff = BigDecimal.ONE.subtract(normSum);
                    normalized.put(last, BigDecimalUtils.toWeight(normalized.get(last).add(diff)));
                }
            }
            return normalized;
        }

        BigDecimal eq = BigDecimalUtils.toWeight(
                BigDecimalUtils.safeDivide(BigDecimal.ONE, BigDecimal.valueOf(weights.size()), BigDecimalUtils.CALC_SCALE));
        Map<String, BigDecimal> equal = new LinkedHashMap<>();
        weights.keySet().forEach(t -> equal.put(t, eq));
        return equal;
    }

    private void buildRecommendations(DiversificationResult result,
                                      Map<String, BigDecimal> sectorWeights,
                                      BigDecimal hhi,
                                      Portfolio portfolio,
                                      Map<String, BigDecimal> suggested) {

        if ("HIGH".equals(result.getConcentrationLevel())) {
            result.addRecommendation(RebalanceRecommendation.withSuggestedWeights(
                    RebalanceRecommendation.Severity.CRITICAL,
                    "High concentration (HHI=" + hhi + ", effective sectors="
                            + result.getEffectiveSectorCount() + "). Reduce weight of largest sectors.",
                    null, hhi, ConcentrationCalculator.HHI_HIGH, suggested
            ));
        } else if ("MEDIUM".equals(result.getConcentrationLevel())) {
            result.addRecommendation(RebalanceRecommendation.withSuggestedWeights(
                    RebalanceRecommendation.Severity.WARNING,
                    "Medium concentration (HHI=" + hhi + "). Monitor largest sectors.",
                    null, hhi, ConcentrationCalculator.HHI_HIGH, suggested
            ));
        } else {
            result.addRecommendation(RebalanceRecommendation.info(
                    "Good sector diversification (HHI=" + hhi
                            + ", effective sectors=" + result.getEffectiveSectorCount() + ")."
            ));
        }

        for (Map.Entry<String, BigDecimal> e : sectorWeights.entrySet()) {
            if (e.getValue().compareTo(MAX_SECTOR_WEIGHT) > 0) {
                result.addRecommendation(RebalanceRecommendation.withSuggestedWeights(
                        RebalanceRecommendation.Severity.WARNING,
                        String.format("Sector %s is %.1f%% of portfolio. Suggested max is %.0f%%.",
                                e.getKey(),
                                PercentageUtils.fractionToPercentDouble(e.getValue()),
                                PercentageUtils.fractionToPercentDouble(MAX_SECTOR_WEIGHT)),
                        e.getKey(),
                        e.getValue(),
                        MAX_SECTOR_WEIGHT,
                        suggested
                ));
            }
        }

        if (sectorWeights.size() < 3 && portfolio.getPositions().size() >= 3) {
            result.addRecommendation(RebalanceRecommendation.warning(
                    "Fewer than 3 sectors. Add names from other industries.",
                    null, null, null
            ));
        }

        BigDecimal total = portfolio.totalWeight();
        if (total.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.02")) > 0) {
            result.addRecommendation(RebalanceRecommendation.warning(
                    "Sum of weights = " + BigDecimalUtils.toWeight(total) + " (expected ≈ 1.0).",
                    null, total, BigDecimal.ONE
            ));
        }

        BigDecimal top1 = result.getTop1Share();
        if (top1 != null && top1.compareTo(MAX_SECTOR_WEIGHT) > 0) {
            result.addRecommendation(RebalanceRecommendation.warning(
                    "Top-1 sector share = " + PercentageUtils.fractionToPercent(top1) + "%.",
                    null, top1, MAX_SECTOR_WEIGHT
            ));
        }
    }

    private String buildSummaryText(DiversificationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("HHI: ").append(result.getHhi())
                .append(" (").append(result.getConcentrationLevel()).append(")")
                .append(", effective sectors: ").append(result.getEffectiveSectorCount())
                .append(", top-1: ").append(PercentageUtils.fractionToPercent(result.getTop1Share())).append("%")
                .append(", top-3: ").append(PercentageUtils.fractionToPercent(result.getTop3Share())).append("%\n\n");

        sb.append("Sector allocation:\n");
        for (SectorAllocation a : result.getSectorAllocations()) {
            sb.append(String.format("  - %s: %s%% (%d pos.)\n",
                    a.getSector(), a.getWeightPercent(), a.getPositionCount()));
        }

        if (result.getSuggestedWeights() != null && !result.getSuggestedWeights().isEmpty()) {
            sb.append("\nSuggested weights:\n");
            result.getSuggestedWeights().forEach((t, w) ->
                    sb.append(String.format("  - %s: %s%%\n", t, PercentageUtils.fractionToPercent(w))));
        }

        sb.append("\nRecommendations:\n");
        for (RebalanceRecommendation r : result.getRecommendations()) {
            sb.append("  [").append(r.getSeverity()).append("] ")
                    .append(r.getMessage()).append("\n");
        }
        return sb.toString();
    }
}
