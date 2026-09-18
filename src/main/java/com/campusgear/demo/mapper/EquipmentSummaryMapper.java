package com.campusgear.demo.mapper;

import com.campusgear.demo.dto.ReservationResponseDTO;
import com.campusgear.demo.entity.EquipmentEntity;

/**
 * Statyczny helper używany przez mappery MapStruct ({@code uses}).
 * Metoda statyczna = generowany kod woła ją bez instancji,
 * więc mappery da się tworzyć gołym {@code new} w testach jednostkowych.
 */
public final class EquipmentSummaryMapper {

    private EquipmentSummaryMapper() {
    }

    public static ReservationResponseDTO.EquipmentSummaryDTO toSummary(EquipmentEntity equipment) {
        if (equipment == null) {
            return null;
        }
        return new ReservationResponseDTO.EquipmentSummaryDTO(
                equipment.getId(),
                equipment.getDeviceType(),
                equipment.getTechnicalSpecification(),
                equipment.getSerialNumber(),
                equipment.getLocation()
        );
    }
}
