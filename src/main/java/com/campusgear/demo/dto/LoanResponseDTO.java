package com.campusgear.demo.dto;

import java.time.LocalDateTime;

public record LoanResponseDTO(
        Long id,
        LocalDateTime borrowDate,
        LocalDateTime expectedReturnDate,
        LocalDateTime returnRequestedAt,
        LocalDateTime actualReturnDate,
        Long reservationId,
        String userEmail,
        ReservationResponseDTO.EquipmentSummaryDTO equipment
) {
}
