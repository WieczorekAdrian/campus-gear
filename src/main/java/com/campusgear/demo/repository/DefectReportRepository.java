package com.campusgear.demo.repository;

import com.campusgear.demo.entity.DefectReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefectReportRepository extends JpaRepository<DefectReportEntity, Long> {
}