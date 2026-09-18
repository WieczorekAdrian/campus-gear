package com.campusgear.demo.service;

import com.campusgear.demo.entity.DefectReportEntity;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.DefectReportEntityRepository;
import com.campusgear.demo.status.DefectStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefectReportService {

    private final DefectReportEntityRepository defectReportRepository;

    @Transactional
    public DefectReportEntity reportDefect(EquipmentEntity equipment, UserEntity reporter, String description) {
        DefectReportEntity defect = new DefectReportEntity();
        defect.setDescription(description != null && !description.isBlank()
                ? description
                : "Uszkodzenie zgłoszone przy zwrocie");
        defect.setReportDate(LocalDateTime.now());
        defect.setStatus(DefectStatus.ZGLOSZONA);
        defect.setEquipment(equipment);
        defect.setReporter(reporter);

        DefectReportEntity saved = defectReportRepository.save(defect);
        log.info("Defect {} reported for equipment {} by {}", saved.getId(), equipment.getId(), reporter.getEmail());

        return saved;
    }
}
