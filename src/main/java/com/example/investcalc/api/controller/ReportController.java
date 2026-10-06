package com.example.investcalc.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.investcalc.api.dto.request.ReportRequest;
import com.example.investcalc.api.dto.response.ReportResponse;
import com.example.investcalc.repository.ReportMetadataRepository;
import com.example.investcalc.repository.entity.ReportMetadata;
import com.example.investcalc.exception.ReportNotFoundException;
import com.example.investcalc.exception.ReportNotReadyException;
import com.example.investcalc.service.OrchestratorService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportMetadataRepository reportMetadataRepository;
    private final OrchestratorService orchestratorService;
    private final ObjectMapper objectMapper;

    public ReportController(ReportMetadataRepository reportMetadataRepository,
                            OrchestratorService orchestratorService,
                            ObjectMapper objectMapper) {
        this.reportMetadataRepository = reportMetadataRepository;
        this.orchestratorService = orchestratorService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/generate")
    public ReportResponse generate(@Valid @RequestBody ReportRequest request) throws Exception {
        ReportMetadata metadata = new ReportMetadata();
        metadata.setStatus(ReportMetadata.Status.PROCESSING);
        metadata.setRequestJson(objectMapper.writeValueAsString(request));
        metadata = reportMetadataRepository.save(metadata);

        orchestratorService.generateReportAsync(metadata.getReportId(), request);

        return new ReportResponse(
                metadata.getReportId(),
                metadata.getStatus().name(),
                null,
                metadata.getCreatedAt()
        );
    }

    @GetMapping("/{reportId}")
    public ReportResponse status(@PathVariable String reportId) {
        ReportMetadata m = reportMetadataRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        String downloadUrl = m.getStatus() == ReportMetadata.Status.COMPLETED
                ? "/api/v1/reports/" + reportId + "/download"
                : null;

        return new ReportResponse(
                m.getReportId(),
                m.getStatus().name(),
                downloadUrl,
                m.getCreatedAt(),
                m.getErrorMessage()
        );
    }

    @GetMapping("/{reportId}/download")
    public ResponseEntity<Resource> download(@PathVariable String reportId) {
        ReportMetadata m = reportMetadataRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        if (m.getStatus() != ReportMetadata.Status.COMPLETED || m.getFilePath() == null) {
            throw new ReportNotReadyException(reportId, m.getStatus().name());
        }

        Resource resource = new FileSystemResource(Path.of(m.getFilePath()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + reportId + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}