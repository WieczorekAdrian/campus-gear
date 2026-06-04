package com.campusgear.demo.repository;

import com.campusgear.demo.entity.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanEntityRepository extends JpaRepository<LoanEntity, Long> {
}