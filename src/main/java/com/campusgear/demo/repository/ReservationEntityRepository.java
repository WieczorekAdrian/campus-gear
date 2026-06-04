package com.campusgear.demo.repository;

import com.campusgear.demo.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationEntityRepository extends JpaRepository<ReservationEntity, Long> {
}