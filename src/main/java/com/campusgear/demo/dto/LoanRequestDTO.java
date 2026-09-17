package com.campusgear.demo.dto;

import jakarta.validation.constraints.NotNull;

public record LoanRequestDTO(
        @NotNull(message = "ID rezerwacji nie może być puste")
        Long reservationId
) {
}
