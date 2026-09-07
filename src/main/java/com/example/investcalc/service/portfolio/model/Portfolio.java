package com.example.investcalc.service.portfolio.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Portfolio {

    private final List<WeightedPosition> positions = new ArrayList<>();
    private final BigDecimal initialCapital;

    public Portfolio(BigDecimal initialCapital) {
        this.initialCapital = initialCapital;
    }

    public void addPosition(WeightedPosition position) {
        positions.add(position);
    }

    public List<WeightedPosition> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public BigDecimal getInitialCapital() {
        return initialCapital;
    }

    public BigDecimal totalWeight() {
        return positions.stream()
                .map(WeightedPosition::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}