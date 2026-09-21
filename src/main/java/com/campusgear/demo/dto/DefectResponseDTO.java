package com.campusgear.demo.dto;

import com.campusgear.demo.status.DefectStatus;

import java.time.LocalDateTime;

public record DefectResponseDTO(
        Long id,
        String description,
        LocalDateTime reportDate,
        DefectStatus status,
        String reporterEmail,
        ReservationResponseDTO.EquipmentSummaryDTO equipment
) {
}
