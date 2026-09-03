package com.example.investcalc.service.report;

import com.example.investcalc.api.dto.request.ReportRequest;
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

        Path dir = Path.of(reportsDir);
        Files.createDirectories(dir);

        Path pdfPath = dir.resolve(reportId + ".pdf");
        Path zipPath = dir.resolve(reportId + ".zip");

        // 1. PDF
        createSimplePdf(pdfPath, reportId, request,
                diversificationSummary, backtestSummary, varSummary);

        // 2. ZIP
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            ZipEntry entry = new ZipEntry(reportId + ".pdf");
            zos.putNextEntry(entry);
            Files.copy(pdfPath, zos);
            zos.closeEntry();
        }

        // PDF можно удалить после упаковки, если не нужен отдельно
        Files.deleteIfExists(pdfPath);

        log.info("Report ZIP created: {}", zipPath.toAbsolutePath());
        return zipPath.toAbsolutePath().toString();
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

        document.add(new Paragraph("Запрос", headingFont));
        document.add(new Paragraph("Капитал: " + request.initialCapital(), normalFont));
        document.add(new Paragraph("Стратегия: " + request.strategy(), normalFont));
        document.add(new Paragraph("Период (лет): " + request.periodYears(), normalFont));
        document.add(new Paragraph("Позиций: " + request.positions().size(), normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("1. Диверсификация", headingFont));
        document.add(new Paragraph(diversificationSummary, normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("2. Бэктест", headingFont));
        document.add(new Paragraph(backtestSummary, normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("3. Риск (VaR)", headingFont));
        document.add(new Paragraph(varSummary, normalFont));

        document.close();
    }
}