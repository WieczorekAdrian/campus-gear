package com.campusgear.demo.service;

import com.campusgear.demo.dto.DefectResponseDTO;
import com.campusgear.demo.entity.DefectReportEntity;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.exception.ReservationConflictException;
import com.campusgear.demo.exception.ResourceNotFoundException;
import com.campusgear.demo.mapper.DefectMapper;
import com.campusgear.demo.repository.DefectReportEntityRepository;
import com.campusgear.demo.status.DefectStatus;
import com.campusgear.demo.status.EquipmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefectReportService {

    private final DefectReportEntityRepository defectReportRepository;
    private final DefectMapper defectMapper;

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

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public List<DefectResponseDTO> getAllDefects(DefectStatus status) {
        List<DefectReportEntity> defects = (status == null)
                ? defectReportRepository.findAllByOrderByReportDateDesc()
                : defectReportRepository.findByStatusOrderByReportDateDesc(status);

        return defects.stream().map(defectMapper::toDto).toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public DefectResponseDTO updateStatus(Long defectId, DefectStatus newStatus, String email) {
        log.info("Updating defect {} to {} by user {}", defectId, newStatus, email);

        DefectReportEntity defect = defectReportRepository.findById(defectId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono usterki o ID: " + defectId));

        if (newStatus.ordinal() < defect.getStatus().ordinal()) {
            throw new ReservationConflictException("Nie można cofnąć statusu usterki.");
        }

        defect.setStatus(newStatus);

        if (newStatus == DefectStatus.NAPRAWIONA && defect.getEquipment() != null) {
            defect.getEquipment().setStatus(EquipmentStatus.DOSTEPNY);
            log.info("Equipment {} back to DOSTEPNY after repair", defect.getEquipment().getId());
        }

        return defectMapper.toDto(defect);
    }
}
