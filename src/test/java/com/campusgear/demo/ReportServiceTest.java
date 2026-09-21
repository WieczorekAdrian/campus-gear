package com.campusgear.demo;

import com.campusgear.demo.dto.ReportSummaryDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.LoanEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.DefectReportEntityRepository;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.LoanEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.service.ReportService;
import com.campusgear.demo.status.EquipmentStatus;
import com.campusgear.demo.status.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private EquipmentEntityRepository equipmentRepository;

    @Mock
    private ReservationEntityRepository reservationRepository;

    @Mock
    private LoanEntityRepository loanRepository;

    @Mock
    private DefectReportEntityRepository defectReportRepository;

    @InjectMocks
    private ReportService reportService;

    private LoanEntity loan(EquipmentEntity equipment, long id) {
        UserEntity user = new UserEntity();
        user.setEmail("student@campus.edu.pl");
        LoanEntity loan = new LoanEntity();
        loan.setId(id);
        loan.setEquipment(equipment);
        loan.setUser(user);
        loan.setBorrowDate(LocalDateTime.now().minusDays(5));
        loan.setExpectedReturnDate(LocalDateTime.now().plusDays(1));
        return loan;
    }

    private EquipmentEntity equipment(long id, String serial) {
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setId(id);
        equipment.setDeviceType("Laptop " + id);
        equipment.setSerialNumber(serial);
        return equipment;
    }

    @Test
    void shouldSummarizeCounts() {
        when(equipmentRepository.count()).thenReturn(8L);
        when(equipmentRepository.countByStatus(EquipmentStatus.DOSTEPNY)).thenReturn(7L);
        when(loanRepository.countByActualReturnDateIsNull()).thenReturn(2L);
        when(loanRepository.countByActualReturnDateIsNullAndExpectedReturnDateBefore(any())).thenReturn(1L);
        when(loanRepository.count()).thenReturn(5L);
        when(loanRepository.findAllByOrderByBorrowDateDesc()).thenReturn(List.of());

        ReportSummaryDTO summary = reportService.getSummary();

        assertThat(summary.equipmentTotal()).isEqualTo(8L);
        assertThat(summary.equipmentByStatus()).containsEntry("DOSTEPNY", 7L);
        assertThat(summary.loansActive()).isEqualTo(2L);
        assertThat(summary.loansOverdue()).isEqualTo(1L);
        assertThat(summary.loansReturned()).isEqualTo(3L);
        assertThat(summary.topEquipment()).isEmpty();
    }

    @Test
    void shouldRankTopEquipment() {
        EquipmentEntity laptopA = equipment(1L, "A-001");
        EquipmentEntity laptopB = equipment(2L, "B-002");
        LoanEntity first = loan(laptopA, 1L);
        LoanEntity second = loan(laptopA, 2L);
        second.setActualReturnDate(LocalDateTime.now());
        when(loanRepository.findAllByOrderByBorrowDateDesc())
                .thenReturn(List.of(first, second, loan(laptopB, 3L)));

        ReportSummaryDTO summary = reportService.getSummary();

        assertThat(summary.topEquipment()).hasSize(2);
        assertThat(summary.topEquipment().get(0).serialNumber()).isEqualTo("A-001");
        assertThat(summary.topEquipment().get(0).loans()).isEqualTo(2L);
    }

    @Test
    void shouldExportCsvWithEscaping() {
        EquipmentEntity tricky = new EquipmentEntity();
        tricky.setDeviceType("Laptop; \"Pro\"");
        tricky.setSerialNumber("X;1");
        UserEntity user = new UserEntity();
        user.setEmail("a@campus.edu.pl");
        LoanEntity loan = new LoanEntity();
        loan.setId(1L);
        loan.setEquipment(tricky);
        loan.setUser(user);
        loan.setBorrowDate(LocalDateTime.of(2026, 10, 1, 10, 0));
        loan.setExpectedReturnDate(LocalDateTime.of(2026, 10, 3, 10, 0));
        when(loanRepository.findAllByOrderByBorrowDateDesc()).thenReturn(List.of(loan));

        String csv = reportService.exportLoansCsv();

        assertThat(csv).startsWith("id;user;deviceType;serialNumber;borrowDate;expectedReturnDate;actualReturnDate;status\n");
        assertThat(csv).contains("\"Laptop; \"\"Pro\"\"\"");
        assertThat(csv).contains("\"X;1\"");
        assertThat(csv).contains(";AKTYWNY\n");
    }
}
