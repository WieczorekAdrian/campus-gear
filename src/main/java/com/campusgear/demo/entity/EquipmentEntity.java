package com.campusgear.demo.entity;

import com.campusgear.demo.status.EquipmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class EquipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceType;
    private String technicalSpecification;
    private String serialNumber;
    private String location;
    @Enumerated(EnumType.STRING)
    private EquipmentStatus status;
    private boolean isAcademicAccount;

}