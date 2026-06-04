package com.campusgear.demo.repository;

import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentEntityRepository extends JpaRepository<EquipmentEntity, Long> {

    // Spring sam domyśli się, że ma szukać po kolumnie "status"
    List<EquipmentEntity> findByStatus(EquipmentStatus status);

    // Spring sam domyśli się, że ma szukać po kolumnie "deviceType"
    List<EquipmentEntity> findByDeviceType(String deviceType);

    List<EquipmentEntity> findByStatusAndDeviceType(EquipmentStatus status, String deviceType);
}