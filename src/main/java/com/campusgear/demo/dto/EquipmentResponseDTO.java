package com.campusgear.demo.dto;

import com.campusgear.demo.status.EquipmentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentResponseDTO {

    // Tutaj ID już jest potrzebne, bo odsyłamy do frontendu informację o tym,
    // pod jakim numerem w bazie zapisał się ten nowy sprzęt.

    private Long id;
    private String deviceType;
    private String technicalSpecification;
    private String serialNumber;
    private String location;
    private EquipmentStatus status;
}