package com.example.investcalc.repository;

import com.example.investcalc.repository.entity.Candle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CandleRepository extends JpaRepository<Candle, Long> {

    List<Candle> findByTickerAndDateBetweenOrderByDateAsc(
            String ticker, LocalDate from, LocalDate to);

    @Query("""
           SELECT c FROM Candle c
           WHERE c.ticker IN :tickers
             AND c.date BETWEEN :from AND :to
           ORDER BY c.ticker, c.date
           """)
    List<Candle> findByTickersAndDateRange(
            @Param("tickers") List<String> tickers,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    boolean existsByTickerAndDate(String ticker, LocalDate date);
}