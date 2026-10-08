package com.example.investcalc.service.diversification.calculator;

import com.example.investcalc.util.BigDecimalUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ConcentrationCalculator {

    public static final BigDecimal HHI_LOW = new BigDecimal("0.15");
    public static final BigDecimal HHI_HIGH = new BigDecimal("0.25");

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    public BigDecimal hhi(Map<String, BigDecimal> sectorWeights) {
        if (sectorWeights == null || sectorWeights.isEmpty()) {
            return BigDecimal.ZERO.setScale(BigDecimalUtils.WEIGHT_SCALE, BigDecimalUtils.ROUNDING);
        }
        BigDecimal sumSq = BigDecimal.ZERO;
        for (BigDecimal w : sectorWeights.values()) {
            BigDecimal weight = BigDecimalUtils.nz(w);
            sumSq = sumSq.add(weight.multiply(weight, MC), MC);
        }
        return BigDecimalUtils.toWeight(sumSq);
    }

    public BigDecimal effectiveNumberOfSectors(BigDecimal hhi) {
        if (hhi == null || hhi.signum() <= 0) {
            return BigDecimal.ZERO.setScale(BigDecimalUtils.WEIGHT_SCALE, BigDecimalUtils.ROUNDING);
        }
        return BigDecimalUtils.toWeight(BigDecimal.ONE.divide(hhi, MC));
    }

    public BigDecimal effectiveNumberOfSectors(Map<String, BigDecimal> sectorWeights) {
        return effectiveNumberOfSectors(hhi(sectorWeights));
    }

    public BigDecimal topNShare(Map<String, BigDecimal> sectorWeights, int n) {
        if (sectorWeights == null || sectorWeights.isEmpty() || n <= 0) {
            return BigDecimal.ZERO.setScale(BigDecimalUtils.WEIGHT_SCALE, BigDecimalUtils.ROUNDING);
        }
        List<BigDecimal> sorted = sectorWeights.values().stream()
                .map(BigDecimalUtils::nz)
                .sorted(Comparator.reverseOrder())
                .limit(n)
                .toList();
        return BigDecimalUtils.toWeight(BigDecimalUtils.sum(sorted));
    }

    public List<Map.Entry<String, BigDecimal>> topN(Map<String, BigDecimal> sectorWeights, int n) {
        if (sectorWeights == null || sectorWeights.isEmpty() || n <= 0) {
            return List.of();
        }
        return sectorWeights.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(n)
                .map(e -> Map.entry(e.getKey(), BigDecimalUtils.toWeight(BigDecimalUtils.nz(e.getValue()))))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public String concentrationLevel(BigDecimal hhi) {
        if (hhi == null) {
            return "HIGH";
        }
        if (hhi.compareTo(HHI_LOW) < 0) {
            return "LOW";
        }
        if (hhi.compareTo(HHI_HIGH) <= 0) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    public String concentrationLevel(Map<String, BigDecimal> sectorWeights) {
        return concentrationLevel(hhi(sectorWeights));
    }
}
