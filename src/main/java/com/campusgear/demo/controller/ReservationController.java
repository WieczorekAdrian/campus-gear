package com.campusgear.demo.controller;

import com.campusgear.demo.dto.ReservationRequestDTO;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<String> createReservation(
            @Valid @RequestBody ReservationRequestDTO dto,
            @AuthenticationPrincipal UserEntity user) { // Magia dzieje się tutaj!

        // Serwis dostaje encję, nie musi wiedzieć skąd
        reservationService.createReservation(dto, user);

        return ResponseEntity.ok("Zarezerwowano pomyślnie");
    }
}