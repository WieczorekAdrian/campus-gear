package com.campusgear.demo.controller;

import com.campusgear.demo.dto.LoanRequestDTO;
import com.campusgear.demo.dto.LoanResponseDTO;
import com.campusgear.demo.dto.LoanReturnDTO;
import com.campusgear.demo.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    public ResponseEntity<LoanResponseDTO> issueLoan(
            @Valid @RequestBody LoanRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        LoanResponseDTO response = loanService.issueLoan(dto.reservationId(), userDetails.getUsername());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/request-return")
    public ResponseEntity<LoanResponseDTO> requestReturn(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(loanService.requestReturn(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/confirm-return")
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public ResponseEntity<LoanResponseDTO> confirmReturn(
            @PathVariable Long id,
            @RequestBody(required = false) LoanReturnDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        LoanResponseDTO response = loanService.confirmReturn(id, userDetails.getUsername(), dto);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<LoanResponseDTO>> getMyLoans(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(loanService.getMyLoans(userDetails.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public ResponseEntity<List<LoanResponseDTO>> getAllLoans(
            @RequestParam(defaultValue = "true") boolean activeOnly) {

        return ResponseEntity.ok(loanService.getAllLoans(activeOnly));
    }
}
