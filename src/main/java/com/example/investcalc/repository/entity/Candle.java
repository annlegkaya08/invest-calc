package com.example.investcalc.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Setter
@Getter
@Entity
@Table(name = "candle",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ticker", "trade_date"}),
        indexes = {
                @Index(name = "idx_candle_ticker_date", columnList = "ticker, trade_date")
        })
public class Candle {

    // getters & setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(name = "trade_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal open;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal high;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal low;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal close;

    @Column(nullable = false)
    private Long volume;

    // --- constructors, getters, setters, equals/hashCode ---

    public Candle() {}

    public Candle(String ticker, LocalDate date, BigDecimal open, BigDecimal high,
                  BigDecimal low, BigDecimal close, Long volume) {
        this.ticker = ticker;
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Candle candle)) return false;
        return Objects.equals(ticker, candle.ticker) && Objects.equals(date, candle.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, date);
    }
}