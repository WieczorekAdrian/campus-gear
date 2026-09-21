package com.campusgear.demo.mapper;

import com.campusgear.demo.dto.DefectResponseDTO;
import com.campusgear.demo.entity.DefectReportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = EquipmentSummaryMapper.class)
public interface DefectMapper {

    @Mapping(target = "reporterEmail", source = "reporter.email")
    DefectResponseDTO toDto(DefectReportEntity defect);
}
