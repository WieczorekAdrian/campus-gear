package com.campusgear.demo.dto;

import com.campusgear.demo.status.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponseDTO(
        Long id,
        LocalDateTime startDate,
        LocalDateTime endDate,
        ReservationStatus status,
        EquipmentSummaryDTO equipment
) {
    public record EquipmentSummaryDTO(
            Long id,
            String deviceType,
            String technicalSpecification,
            String serialNumber,
            String location
    ) {
    }
}
