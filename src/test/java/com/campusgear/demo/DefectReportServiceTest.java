package com.campusgear.demo;

import com.campusgear.demo.entity.DefectReportEntity;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.DefectReportEntityRepository;
import com.campusgear.demo.service.DefectReportService;
import com.campusgear.demo.status.DefectStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefectReportServiceTest {

    @Mock
    private DefectReportEntityRepository defectReportRepository;

    @InjectMocks
    private DefectReportService defectReportService;

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
}
