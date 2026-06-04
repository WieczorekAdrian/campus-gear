package com.campusgear.demo.mapper;

import com.campusgear.demo.dto.EquipmentRequestDTO;
import com.campusgear.demo.dto.EquipmentResponseDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

// componentModel = "spring" pozwala wstrzykiwać ten mapper do innych klas tak jak Repozytoria
@Mapper(componentModel = "spring")
public interface EquipmentMapper {

    // 1. Zamiana Request DTO na Encję (do zapisu)
    EquipmentEntity toEntity(EquipmentRequestDTO dto);

    // 2. Zamiana Encji na Response DTO (do zwrotu na frontend)
    EquipmentResponseDTO toResponseDTO(EquipmentEntity entity);

    // 3. Aktualizacja istniejącej encji (przy edycji) - genialna funkcja MapStructa!
    void updateEntityFromDto(EquipmentRequestDTO dto, @MappingTarget EquipmentEntity entity);
}