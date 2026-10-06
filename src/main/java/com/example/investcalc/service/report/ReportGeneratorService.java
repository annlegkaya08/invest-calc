package com.example.investcalc.service.report;

import com.example.investcalc.api.dto.request.ReportRequest;
import com.example.investcalc.util.FileUtils;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ReportGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ReportGeneratorService.class);

    @Value("${app.reports.dir:./reports}")
    private String reportsDir;

    public String generate(String reportId,
                           ReportRequest request,
                           String diversificationSummary,
                           String backtestSummary,
                           String varSummary) throws IOException {

        Path dir = FileUtils.ensureDirectory(reportsDir);
        log.info("Reports directory: {}", dir);

        Path pdfPath = FileUtils.safeResolve(dir, reportId + ".pdf");
        Path zipPath = FileUtils.safeResolve(dir, reportId + ".zip");

        createSimplePdf(pdfPath, reportId, request,
                diversificationSummary, backtestSummary, varSummary);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zos.putNextEntry(new ZipEntry(reportId + ".pdf"));
            Files.copy(pdfPath, zos);
            zos.closeEntry();
        }

        FileUtils.deleteQuietly(pdfPath);

        log.info("ZIP created: {}", zipPath);
        return zipPath.toString();
    }

    private void createSimplePdf(Path pdfPath,
                                 String reportId,
                                 ReportRequest request,
                                 String diversificationSummary,
                                 String backtestSummary,
                                 String varSummary) throws IOException {

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(pdfPath.toFile()));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        document.add(new Paragraph("Investment Report", titleFont));
        document.add(new Paragraph("Report ID: " + reportId, normalFont));
        document.add(new Paragraph("Generated: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Request", headingFont));
        document.add(new Paragraph("Capital: " + request.initialCapital(), normalFont));
        document.add(new Paragraph("Strategy: " + request.strategy(), normalFont));
        document.add(new Paragraph("Period (years): " + request.periodYears(), normalFont));
        document.add(new Paragraph("Positions: " + request.positions().size(), normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("1. Diversification", headingFont));
        document.add(new Paragraph(diversificationSummary, normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("2. Backtest", headingFont));
        document.add(new Paragraph(backtestSummary, normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("3. Risk (VaR)", headingFont));
        document.add(new Paragraph(varSummary, normalFont));

        document.close();
    }
}