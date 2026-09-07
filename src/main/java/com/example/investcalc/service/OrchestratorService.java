package com.example.investcalc.service;

import com.example.investcalc.api.dto.request.ReportRequest;
import com.example.investcalc.repository.ReportMetadataRepository;
import com.example.investcalc.repository.entity.ReportMetadata;
import com.example.investcalc.service.diversification.calculator.PortfolioAnalyzer;
import com.example.investcalc.service.diversification.model.DiversificationResult;
import com.example.investcalc.service.report.ReportGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);

    private final ReportGeneratorService reportGeneratorService;
    private final ReportMetadataRepository reportMetadataRepository;
    private final PortfolioAnalyzer portfolioAnalyzer;

    public OrchestratorService(ReportGeneratorService reportGeneratorService,
                               ReportMetadataRepository reportMetadataRepository,
                               PortfolioAnalyzer portfolioAnalyzer) {
        this.reportGeneratorService = reportGeneratorService;
        this.reportMetadataRepository = reportMetadataRepository;
        this.portfolioAnalyzer = portfolioAnalyzer;
    }

    @Async
    public void generateReportAsync(String reportId, ReportRequest request) {
        log.info(">>> Start report generation: {}", reportId);

        ReportMetadata metadata = reportMetadataRepository.findById(reportId).orElse(null);
        if (metadata == null) {
            log.error("ReportMetadata not found: {}", reportId);
            return;
        }

        try {
            // Модуль 14
            DiversificationResult diversification = portfolioAnalyzer.analyze(request);
            String diversificationSummary = diversification.getSummaryText();

            // Модуль 11 (заглушка)
            String backtestSummary = "Backtest: stub";

            // Модуль 6 (заглушка)
            String varSummary = "VaR: stub";

            String filePath = reportGeneratorService.generate(
                    reportId,
                    request,
                    diversificationSummary,
                    backtestSummary,
                    varSummary
            );

            metadata.setStatus(ReportMetadata.Status.COMPLETED);
            metadata.setFilePath(filePath);
            metadata.setCompletedAt(Instant.now());
            metadata.setErrorMessage(null);
            reportMetadataRepository.save(metadata);

            log.info(">>> Report {} COMPLETED: {}", reportId, filePath);

        } catch (Exception e) {
            log.error(">>> Report {} FAILED", reportId, e);

            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            if (msg.length() > 900) {
                msg = msg.substring(0, 900);
            }

            metadata.setStatus(ReportMetadata.Status.FAILED);
            metadata.setErrorMessage(msg);
            metadata.setCompletedAt(Instant.now());
            reportMetadataRepository.save(metadata);
        }
    }
}