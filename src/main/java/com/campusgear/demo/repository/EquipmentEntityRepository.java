package com.campusgear.demo.repository;

import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.status.EquipmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentEntityRepository extends JpaRepository<EquipmentEntity, Long>,
        JpaSpecificationExecutor<EquipmentEntity> {

    long countByStatus(EquipmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EquipmentEntity e where e.id = :id")
    Optional<EquipmentEntity> findAndLockById(@Param("id") Long id);
}