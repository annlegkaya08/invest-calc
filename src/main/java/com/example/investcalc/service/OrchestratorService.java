package com.example.investcalc.service;

import com.example.investcalc.api.dto.request.ReportRequest;
import com.example.investcalc.repository.entity.ReportMetadata;
import com.example.investcalc.service.report.ReportGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);

    private final ReportGeneratorService reportGeneratorService;

    public OrchestratorService(ReportGeneratorService reportGeneratorService) {
        this.reportGeneratorService = reportGeneratorService;
    }

    @Async
    public void generateReportAsync(ReportMetadata metadata, ReportRequest request) {
        try {
            log.info("Start report generation: {}", metadata.getReportId());
            //заглушки для модулей
            String diversificationSummary = runDiversification(request);

            String backtestSummary = runBacktest(request);

            String varSummary = runVaR(request);

            // === Генерация ZIP + PDF ===
            String filePath = reportGeneratorService.generate(
                    metadata.getReportId(),
                    request,
                    diversificationSummary,
                    backtestSummary,
                    varSummary
            );

            metadata.setStatus(ReportMetadata.Status.COMPLETED);
            metadata.setFilePath(filePath);
            metadata.setCompletedAt(java.time.Instant.now());

            log.info("Report {} completed: {}", metadata.getReportId(), filePath);

        } catch (Exception e) {
            log.error("Report generation failed: {}", metadata.getReportId(), e);
            metadata.setStatus(ReportMetadata.Status.FAILED);
            metadata.setErrorMessage(e.getMessage());
        }
    }

    private String runDiversification(ReportRequest request) {
        // TODO: PortfolioAnalyzer
        return "Диверсификация: заглушка (секторы будут здесь)";
    }

    private String runBacktest(ReportRequest request) {
        // TODO: BacktestingEngine
        return "Бэктест: заглушка (доходность и сделки будут здесь)";
    }

    private String runVaR(ReportRequest request) {
        // TODO: VaRCalculator
        return "VaR: заглушка (риск будет здесь)";
    }
}