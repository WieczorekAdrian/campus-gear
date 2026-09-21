package com.campusgear.demo.repository;

import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.status.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationEntityRepository extends JpaRepository<ReservationEntity, Long> {

    long countByStatus(ReservationStatus status);

    boolean existsByEquipmentIdAndStartDateLessThanAndEndDateGreaterThan(
            Long equipmentId,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    boolean existsByEquipmentIdAndStartDateLessThanAndEndDateGreaterThanAndStatusIn(
            Long equipmentId,
            LocalDateTime endDate,
            LocalDateTime startDate,
            java.util.Collection<ReservationStatus> statuses
    );

    @EntityGraph(attributePaths = "equipment")
    List<ReservationEntity> findByUser_EmailOrderByStartDateDesc(String email);

    @EntityGraph(attributePaths = "equipment")
    List<ReservationEntity> findByUser_EmailAndStatusOrderByStartDateDesc(String email, ReservationStatus status);

    @EntityGraph(attributePaths = {"equipment", "user"})
    List<ReservationEntity> findByStatusOrderByStartDateDesc(ReservationStatus status);

    @EntityGraph(attributePaths = {"equipment", "user"})
    List<ReservationEntity> findAllByOrderByStartDateDesc();

}