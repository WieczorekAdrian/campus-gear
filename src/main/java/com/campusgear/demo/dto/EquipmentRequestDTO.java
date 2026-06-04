package com.campusgear.demo.dto;

import com.campusgear.demo.status.EquipmentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentRequestDTO {

    // Zauważ, że nie ma tutaj pola ID!
    // ID nadaje baza danych przy dodawaniu, więc nie chcemy, aby frontend nam je przysyłał.

    private String deviceType;
    private String technicalSpecification;
    private String serialNumber;
    private String location;
    private EquipmentStatus status;
}