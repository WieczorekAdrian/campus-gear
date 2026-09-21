package com.campusgear.demo.controller;

import com.campusgear.demo.dto.ReportSummaryDTO;
import com.campusgear.demo.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public ResponseEntity<ReportSummaryDTO> getSummary() {
        return ResponseEntity.ok(reportService.getSummary());
    }

    @GetMapping(value = "/loans.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public ResponseEntity<byte[]> exportLoansCsv() {
        byte[] csv = reportService.exportLoansCsv().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                        .attachment()
                        .filename("wypozyczenia.csv", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
