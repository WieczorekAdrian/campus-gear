package com.campusgear.demo.mapper;

import com.campusgear.demo.dto.LoanResponseDTO;
import com.campusgear.demo.entity.LoanEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = EquipmentSummaryMapper.class)
public interface LoanMapper {

    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "reservationId", source = "reservation.id")
    LoanResponseDTO toDto(LoanEntity loan);
}
