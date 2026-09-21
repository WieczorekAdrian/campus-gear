package com.campusgear.demo;

import com.campusgear.demo.dto.DefectResponseDTO;
import com.campusgear.demo.entity.DefectReportEntity;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.exception.ReservationConflictException;
import com.campusgear.demo.exception.ResourceNotFoundException;
import com.campusgear.demo.mapper.DefectMapperImpl;
import com.campusgear.demo.repository.DefectReportEntityRepository;
import com.campusgear.demo.service.DefectReportService;
import com.campusgear.demo.status.DefectStatus;
import com.campusgear.demo.status.EquipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefectReportServiceTest {

    @Mock
    private DefectReportEntityRepository defectReportRepository;

    private DefectReportService defectReportService;

    @BeforeEach
    void initService() {
        defectReportService = new DefectReportService(defectReportRepository, new DefectMapperImpl());
    }

    private DefectReportEntity defect(Long id, DefectStatus status) {
        DefectReportEntity defect = new DefectReportEntity();
        defect.setId(id);
        defect.setDescription("Pęknięta obudowa");
        defect.setReportDate(LocalDateTime.now().minusDays(1));
        defect.setStatus(status);
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setId(1L);
        equipment.setDeviceType("Laptop");
        equipment.setSerialNumber("DLL-001");
        equipment.setStatus(EquipmentStatus.SERWISOWANY);
        defect.setEquipment(equipment);
        UserEntity reporter = new UserEntity();
        reporter.setEmail("student@campus.edu.pl");
        defect.setReporter(reporter);
        return defect;
    }

    @Test
    void shouldSaveDefectWithGivenDescription() {
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setId(1L);
        UserEntity reporter = new UserEntity();
        reporter.setEmail("opiekun@campus.edu.pl");
        when(defectReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DefectReportEntity result =
                defectReportService.reportDefect(equipment, reporter, "Pęknięta obudowa");

        assertThat(result.getDescription()).isEqualTo("Pęknięta obudowa");
        assertThat(result.getStatus()).isEqualTo(DefectStatus.ZGLOSZONA);
        assertThat(result.getReportDate()).isNotNull();
        assertThat(result.getEquipment()).isSameAs(equipment);
        assertThat(result.getReporter()).isSameAs(reporter);
    }

    @Test
    void shouldFallBackToDefaultDescription() {
        when(defectReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DefectReportEntity result = defectReportService.reportDefect(
                new EquipmentEntity(), new UserEntity(), "   ");

        ArgumentCaptor<DefectReportEntity> captor = ArgumentCaptor.forClass(DefectReportEntity.class);
        verify(defectReportRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Uszkodzenie zgłoszone przy zwrocie");
        assertThat(result.getStatus()).isEqualTo(DefectStatus.ZGLOSZONA);
    }

    @Test
    void shouldReturnAllDefects() {
        when(defectReportRepository.findAllByOrderByReportDateDesc())
                .thenReturn(List.of(defect(9L, DefectStatus.ZGLOSZONA)));

        List<DefectResponseDTO> result = defectReportService.getAllDefects(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).reporterEmail()).isEqualTo("student@campus.edu.pl");
        assertThat(result.get(0).equipment().serialNumber()).isEqualTo("DLL-001");
    }

    @Test
    void shouldMoveDefectToRepair() {
        DefectReportEntity defect = defect(9L, DefectStatus.ZGLOSZONA);
        when(defectReportRepository.findById(9L)).thenReturn(Optional.of(defect));

        DefectResponseDTO result =
                defectReportService.updateStatus(9L, DefectStatus.W_NAPRAWIE, "opiekun@campus.edu.pl");

        assertThat(result.status()).isEqualTo(DefectStatus.W_NAPRAWIE);
        assertThat(defect.getEquipment().getStatus()).isEqualTo(EquipmentStatus.SERWISOWANY);
    }

    @Test
    void shouldCloseDefectAndReleaseEquipment() {
        DefectReportEntity defect = defect(9L, DefectStatus.W_NAPRAWIE);
        when(defectReportRepository.findById(9L)).thenReturn(Optional.of(defect));

        DefectResponseDTO result =
                defectReportService.updateStatus(9L, DefectStatus.NAPRAWIONA, "opiekun@campus.edu.pl");

        assertThat(result.status()).isEqualTo(DefectStatus.NAPRAWIONA);
        assertThat(defect.getEquipment().getStatus()).isEqualTo(EquipmentStatus.DOSTEPNY);
    }

    @Test
    void shouldRejectStatusRegression() {
        DefectReportEntity defect = defect(9L, DefectStatus.NAPRAWIONA);
        when(defectReportRepository.findById(9L)).thenReturn(Optional.of(defect));

        assertThatThrownBy(() -> defectReportService.updateStatus(9L, DefectStatus.W_NAPRAWIE, "opiekun@campus.edu.pl"))
                .isInstanceOf(ReservationConflictException.class);
    }

    @Test
    void shouldThrowWhenDefectNotFound() {
        when(defectReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> defectReportService.updateStatus(99L, DefectStatus.W_NAPRAWIE, "opiekun@campus.edu.pl"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
