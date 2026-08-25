package com.example.investcalc.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Setter
@Getter
@Entity
@Table(name = "ticker_metadata")
public class TickerMetadata {

    // getters & setters
    //secid
    @Id
    @Column(length = 20)
    private String ticker;

    // shortname/full name
    @Column(nullable = false)
    private String name;

    // OIL_GAS, FINANCE... or UNKNOWN
    @Column(length = 50)
    private String sector;

    @Column(length = 10)
    private String currency = "RUB";

    //main
    @Column(length = 20)
    private String board = "TQBR";

    @Column(length = 20)
    private String isin;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public TickerMetadata() {}

    public TickerMetadata(String ticker, String name, String sector) {
        this.ticker = ticker;
        this.name = name;
        this.sector = sector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TickerMetadata that)) return false;
        return Objects.equals(ticker, that.ticker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker);
    }
}