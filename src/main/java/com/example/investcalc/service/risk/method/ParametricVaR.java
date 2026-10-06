package com.example.investcalc.service.risk.method;

import com.example.investcalc.util.BigDecimalUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

@Component
public class ParametricVaR implements VaRMethod {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
    private double zScore(BigDecimal confidenceLevel) {
        double c = confidenceLevel.doubleValue();
        if (c >= 0.995) return 2.58;
        if (c >= 0.99) return 2.33;
        if (c >= 0.975) return 1.96;
        if (c >= 0.95) return 1.65;
        return 1.65;
    }

    @Override
    public BigDecimal calculateVaR(List<BigDecimal> returns, BigDecimal confidenceLevel) {
        if (returns == null || returns.size() < 2) {
            return BigDecimal.ZERO;
        }

        Stats stats = stats(returns);
        double z = zScore(confidenceLevel);

        // VaR ≈ -(μ - z*σ)  при μ≈0 часто берут z*σ
        // Классика: VaR = z * σ - μ  (если смотрим на потери)
        double var = z * stats.std - stats.mean;
        return BigDecimalUtils.toRatio(BigDecimal.valueOf(Math.max(0, var)));
    }

    @Override
    public BigDecimal calculateES(List<BigDecimal> returns, BigDecimal confidenceLevel) {
        if (returns == null || returns.size() < 2) {
            return BigDecimal.ZERO;
        }

        Stats stats = stats(returns);
        double z = zScore(confidenceLevel);
        double alpha = 1.0 - confidenceLevel.doubleValue();

        // Для нормального: ES = σ * φ(z) / α - μ
        double phi = normalPdf(z);
        double es = stats.std * (phi / alpha) - stats.mean;
        return BigDecimalUtils.toRatio(BigDecimal.valueOf(Math.max(0, es)));
    }

    private Stats stats(List<BigDecimal> returns) {
        int n = returns.size();
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal r : returns) {
            sum = sum.add(r);
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(n), MC);
        BigDecimal varSum = BigDecimal.ZERO;
        for (BigDecimal r : returns) {
            BigDecimal d = r.subtract(mean);
            varSum = varSum.add(d.multiply(d, MC));
        }
        BigDecimal variance = varSum.divide(BigDecimal.valueOf(n - 1L), MC);
        double std = Math.sqrt(variance.doubleValue());
        return new Stats(mean.doubleValue(), std);
    }

    private double normalPdf(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
    }

    private record Stats(double mean, double std) {}
}