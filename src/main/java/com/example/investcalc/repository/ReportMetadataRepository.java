package com.example.investcalc.repository;

import com.example.investcalc.repository.entity.ReportMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportMetadataRepository extends JpaRepository<ReportMetadata, String> {
}