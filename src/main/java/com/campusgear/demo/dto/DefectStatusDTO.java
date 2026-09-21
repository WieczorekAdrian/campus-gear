package com.campusgear.demo.dto;

import com.campusgear.demo.status.DefectStatus;
import jakarta.validation.constraints.NotNull;

public record DefectStatusDTO(
        @NotNull(message = "Status nie może być pusty")
        DefectStatus status
) {
}
