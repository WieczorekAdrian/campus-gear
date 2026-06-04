package com.campusgear.demo.entity;

import com.campusgear.demo.status.EquipmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class EquipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL)
    private List<ReservationEntity> reservations;

    private String deviceType;
    private String technicalSpecification;
    private String serialNumber;
    private String location;
    @Enumerated(EnumType.STRING)
    private EquipmentStatus status;
    private boolean isAcademicAccount;

    private Integer maxRentalDays;

}