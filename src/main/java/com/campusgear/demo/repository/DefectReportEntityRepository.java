package com.campusgear.demo.repository;

import com.campusgear.demo.entity.DefectReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefectReportEntityRepository extends JpaRepository<DefectReportEntity, Long> {
}