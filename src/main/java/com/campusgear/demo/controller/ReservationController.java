package com.campusgear.demo.controller;

import com.campusgear.demo.dto.ReservationRequestDTO;
import com.campusgear.demo.dto.ReservationResponseDTO;
import com.campusgear.demo.service.ReservationService;
import com.campusgear.demo.status.ReservationStatus;
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
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponseDTO> createReservation(
            @Valid @RequestBody ReservationRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        ReservationResponseDTO response =
                reservationService.createReservation(dto, userDetails.getUsername());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponseDTO> cancelReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        ReservationResponseDTO response =
                reservationService.cancelReservation(id, userDetails.getUsername());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ReservationResponseDTO>> getMyReservations(
            @RequestParam(required = false) ReservationStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ReservationResponseDTO> response =
                reservationService.getMyReservations(userDetails.getUsername(), status);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public ResponseEntity<List<ReservationResponseDTO>> getAllReservations(
            @RequestParam(required = false) ReservationStatus status) {

        return ResponseEntity.ok(reservationService.getAllReservations(status));
    }
}
