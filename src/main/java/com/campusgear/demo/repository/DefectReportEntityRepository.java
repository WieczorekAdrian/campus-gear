package com.campusgear.demo.repository;

import com.campusgear.demo.entity.DefectReportEntity;
import com.campusgear.demo.status.DefectStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefectReportEntityRepository extends JpaRepository<DefectReportEntity, Long> {

    @EntityGraph(attributePaths = {"equipment", "reporter"})
    List<DefectReportEntity> findByStatusOrderByReportDateDesc(DefectStatus status);

    @EntityGraph(attributePaths = {"equipment", "reporter"})
    List<DefectReportEntity> findAllByOrderByReportDateDesc();
}