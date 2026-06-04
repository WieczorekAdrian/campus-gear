package com.campusgear.demo.entity;

import com.campusgear.demo.status.DefectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class DefectReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime reportDate;

    @Enumerated(EnumType.STRING)
    private DefectStatus status;

    @ManyToOne
    @JoinColumn(name = "equipment_id", nullable = false)
    private EquipmentEntity equipment;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;
}