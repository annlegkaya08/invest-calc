package com.example.investcalc.repository;

import com.example.investcalc.repository.entity.TickerMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TickerMetadataRepository extends JpaRepository<TickerMetadata, String> {

    Optional<TickerMetadata> findByTickerIgnoreCase(String ticker);

    @Query("""
           SELECT t FROM TickerMetadata t
           WHERE LOWER(t.ticker) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))
           ORDER BY t.ticker
           """)
    List<TickerMetadata> search(String query);
}