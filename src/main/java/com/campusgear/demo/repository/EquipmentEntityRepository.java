package com.campusgear.demo.repository;

import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.status.EquipmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentEntityRepository extends JpaRepository<EquipmentEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EquipmentEntity e where e.id = :id")
    Optional<EquipmentEntity> findAndLockById(@Param("id") Long id);

    // Spring sam domyśli się, że ma szukać po kolumnie "status"
    List<EquipmentEntity> findByStatus(EquipmentStatus status);

    // Spring sam domyśli się, że ma szukać po kolumnie "deviceType"
    List<EquipmentEntity> findByDeviceType(String deviceType);

    List<EquipmentEntity> findByStatusAndDeviceType(EquipmentStatus status, String deviceType);
}