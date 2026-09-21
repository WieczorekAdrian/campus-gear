package com.campusgear.demo.controller;

import com.campusgear.demo.dto.DefectResponseDTO;
import com.campusgear.demo.dto.DefectStatusDTO;
import com.campusgear.demo.service.DefectReportService;
import com.campusgear.demo.status.DefectStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/defects")
@RequiredArgsConstructor
public class DefectController {

    private final DefectReportService defectReportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public ResponseEntity<List<DefectResponseDTO>> getAllDefects(
            @RequestParam(required = false) DefectStatus status) {

        return ResponseEntity.ok(defectReportService.getAllDefects(status));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public ResponseEntity<DefectResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody DefectStatusDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                defectReportService.updateStatus(id, dto.status(), userDetails.getUsername()));
    }
}
