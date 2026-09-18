package com.campusgear.demo.mapper;

import com.campusgear.demo.dto.ReservationResponseDTO;
import com.campusgear.demo.entity.ReservationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = EquipmentSummaryMapper.class)
public interface ReservationMapper {

    @Mapping(target = "userEmail", source = "user.email")
    ReservationResponseDTO toDto(ReservationEntity reservation);
}
