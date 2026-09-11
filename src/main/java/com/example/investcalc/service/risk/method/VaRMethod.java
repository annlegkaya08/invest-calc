package com.example.investcalc.service.risk.method;

import java.math.BigDecimal;
import java.util.List;

public interface VaRMethod {

    BigDecimal calculateVaR(List<BigDecimal> returns, BigDecimal confidenceLevel);

    BigDecimal calculateES(List<BigDecimal> returns, BigDecimal confidenceLevel);
}