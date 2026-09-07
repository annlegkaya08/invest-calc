package com.example.investcalc.service.diversification.calculator;

import com.example.investcalc.api.dto.request.PositionDto;
import com.example.investcalc.api.dto.request.ReportRequest;
import com.example.investcalc.service.diversification.SectorMappingService;
import com.example.investcalc.service.diversification.model.*;
import com.example.investcalc.service.portfolio.model.Portfolio;
import com.example.investcalc.service.portfolio.model.WeightedPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PortfolioAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(PortfolioAnalyzer.class);

    private static final BigDecimal MAX_SECTOR_WEIGHT = new BigDecimal("0.40");
    private static final BigDecimal HHI_LOW = new BigDecimal("0.15");
    private static final BigDecimal HHI_HIGH = new BigDecimal("0.25");

    private final SectorMappingService sectorMappingService;

    public PortfolioAnalyzer(SectorMappingService sectorMappingService) {
        this.sectorMappingService = sectorMappingService;
    }

    public DiversificationResult analyze(ReportRequest request) {
        Portfolio portfolio = buildPortfolio(request);
        enrichWithSectors(portfolio);

        DiversificationResult result = new DiversificationResult();

        Map<String, BigDecimal> sectorWeights = calcSectorWeights(portfolio);
        Map<String, Long> sectorCounts = portfolio.getPositions().stream()
                .collect(Collectors.groupingBy(
                        p -> p.getSector() != null ? p.getSector() : "UNKNOWN",
                        Collectors.counting()
                ));

        sectorWeights.forEach((sector, weight) -> {
            int count = sectorCounts.getOrDefault(sector, 0L).intValue();
            result.addAllocation(new SectorAllocation(sector, weight, count));
        });

        result.getSectorAllocations().sort(
                Comparator.comparing(SectorAllocation::getWeight).reversed()
        );

        BigDecimal hhi = calcHhi(sectorWeights);
        result.setHhi(hhi);
        result.setConcentrationLevel(resolveConcentrationLevel(hhi));

        buildRecommendations(result, sectorWeights, hhi, portfolio);
        result.setSummaryText(buildSummaryText(result));

        log.info("Diversification done: HHI={}, level={}, sectors={}",
                hhi, result.getConcentrationLevel(), sectorWeights.size());

        return result;
    }

    private Portfolio buildPortfolio(ReportRequest request) {
        Portfolio portfolio = new Portfolio(request.initialCapital());
        for (PositionDto dto : request.positions()) {
            portfolio.addPosition(new WeightedPosition(dto.ticker(), dto.weight()));
        }
        return portfolio;
    }

    //временный лог
    private void enrichWithSectors(Portfolio portfolio) {
        for (WeightedPosition pos : portfolio.getPositions()) {
            String sector = sectorMappingService.resolveSector(pos.getTicker());
            pos.setSector(sector);
            log.info("Ticker {} → sector {}", pos.getTicker(), sector);
        }
    }
    private Map<String, BigDecimal> calcSectorWeights(Portfolio portfolio) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (WeightedPosition pos : portfolio.getPositions()) {
            String sector = pos.getSector() != null ? pos.getSector() : "UNKNOWN";
            map.merge(sector, pos.getWeight(), BigDecimal::add);
        }
        return map;
    }

    private BigDecimal calcHhi(Map<String, BigDecimal> sectorWeights) {
        BigDecimal hhi = BigDecimal.ZERO;
        for (BigDecimal w : sectorWeights.values()) {
            hhi = hhi.add(w.multiply(w));
        }
        return hhi.setScale(4, RoundingMode.HALF_UP);
    }

    private String resolveConcentrationLevel(BigDecimal hhi) {
        if (hhi.compareTo(HHI_LOW) < 0) return "LOW";
        if (hhi.compareTo(HHI_HIGH) <= 0) return "MEDIUM";
        return "HIGH";
    }

    private void buildRecommendations(DiversificationResult result,
                                      Map<String, BigDecimal> sectorWeights,
                                      BigDecimal hhi,
                                      Portfolio portfolio) {

        if ("HIGH".equals(result.getConcentrationLevel())) {
            result.addRecommendation(RebalanceRecommendation.critical(
                    "High concentration (HHI=" + hhi + "). Reduce weight of largest sectors.",
                    null, hhi, HHI_HIGH
            ));
        } else if ("MEDIUM".equals(result.getConcentrationLevel())) {
            result.addRecommendation(RebalanceRecommendation.warning(
                    "Medium concentration (HHI=" + hhi + "). Monitor largest sectors.",
                    null, hhi, HHI_HIGH
            ));
        } else {
            result.addRecommendation(RebalanceRecommendation.info(
                    "Good sector diversification (HHI=" + hhi + ")."
            ));
        }

        for (Map.Entry<String, BigDecimal> e : sectorWeights.entrySet()) {
            if (e.getValue().compareTo(MAX_SECTOR_WEIGHT) > 0) {
                result.addRecommendation(RebalanceRecommendation.warning(
                        String.format("Sector %s is %.1f%% of portfolio. Suggested max is %.0f%%.",
                                e.getKey(),
                                e.getValue().multiply(BigDecimal.valueOf(100)).doubleValue(),
                                MAX_SECTOR_WEIGHT.multiply(BigDecimal.valueOf(100)).doubleValue()),
                        e.getKey(),
                        e.getValue(),
                        MAX_SECTOR_WEIGHT
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
                    "Sum of weights = " + total.setScale(4, RoundingMode.HALF_UP) + " (expected ≈ 1.0).",
                    null, total, BigDecimal.ONE
            ));
        }
    }

    private String buildSummaryText(DiversificationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("HHI: ").append(result.getHhi())
                .append(" (").append(result.getConcentrationLevel()).append(")\n\n");

        sb.append("Sector allocation:\n");
        for (SectorAllocation a : result.getSectorAllocations()) {
            sb.append(String.format("  - %s: %s%% (%d pos.)\n",
                    a.getSector(), a.getWeightPercent(), a.getPositionCount()));
        }

        sb.append("\nRecommendations:\n");
        for (RebalanceRecommendation r : result.getRecommendations()) {
            sb.append("  [").append(r.getSeverity()).append("] ")
                    .append(r.getMessage()).append("\n");
        }
        return sb.toString();
    }

}