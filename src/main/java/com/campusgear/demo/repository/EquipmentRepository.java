package com.campusgear.demo.repository;

import com.campusgear.demo.entity.EquipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<EquipmentEntity, Long> {
    // Tutaj kolega będzie dopisywał metody do wyszukiwania, np. findByDeviceType(...)
}