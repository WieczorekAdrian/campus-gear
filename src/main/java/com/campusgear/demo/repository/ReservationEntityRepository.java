package com.campusgear.demo.repository;

import com.campusgear.demo.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ReservationEntityRepository extends JpaRepository<ReservationEntity, Long> {

    boolean existsByEquipmentIdAndStartDateLessThanAndEndDateGreaterThan(
            Long equipmentId,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

}