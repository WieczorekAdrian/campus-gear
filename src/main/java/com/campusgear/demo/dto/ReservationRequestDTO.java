package com.campusgear.demo.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReservationRequestDTO(
        @NotNull(message = "ID sprzętu nie może być puste")
        Long equipmentId,

        @NotNull(message = "Data startu jest wymagana")
        @FutureOrPresent(message = "Data startu nie może być z przeszłości")
        LocalDateTime startDate,

        @NotNull(message = "Data końca jest wymagana")
        @Future(message = "Data końca musi być w przyszłości")
        LocalDateTime endDate
) {}