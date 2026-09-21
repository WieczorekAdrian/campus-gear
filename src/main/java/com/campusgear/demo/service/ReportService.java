package com.campusgear.demo.service;

import com.campusgear.demo.dto.ReportSummaryDTO;
import com.campusgear.demo.entity.LoanEntity;
import com.campusgear.demo.repository.DefectReportEntityRepository;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.LoanEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.status.DefectStatus;
import com.campusgear.demo.status.EquipmentStatus;
import com.campusgear.demo.status.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EquipmentEntityRepository equipmentRepository;
    private final ReservationEntityRepository reservationRepository;
    private final LoanEntityRepository loanRepository;
    private final DefectReportEntityRepository defectReportRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public ReportSummaryDTO getSummary() {
        Map<String, Long> equipmentByStatus = new LinkedHashMap<>();
        for (EquipmentStatus status : EquipmentStatus.values()) {
            equipmentByStatus.put(status.name(), equipmentRepository.countByStatus(status));
        }

        Map<String, Long> reservationsByStatus = new LinkedHashMap<>();
        for (ReservationStatus status : ReservationStatus.values()) {
            reservationsByStatus.put(status.name(), reservationRepository.countByStatus(status));
        }

        Map<String, Long> defectsByStatus = new LinkedHashMap<>();
        for (DefectStatus status : DefectStatus.values()) {
            defectsByStatus.put(status.name(), defectReportRepository.countByStatus(status));
        }

        long loansActive = loanRepository.countByActualReturnDateIsNull();
        long loansOverdue = loanRepository
                .countByActualReturnDateIsNullAndExpectedReturnDateBefore(LocalDateTime.now());
        long loansReturned = loanRepository.count() - loansActive;

        List<LoanEntity> allLoans = loanRepository.findAllByOrderByBorrowDateDesc();

        List<ReportSummaryDTO.TopEquipmentDTO> topEquipment = allLoans.stream()
                .filter(loan -> loan.getEquipment() != null)
                .collect(Collectors.groupingBy(
                        loan -> loan.getEquipment().getId(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> {
                    LoanEntity sample = allLoans.stream()
                            .filter(loan -> loan.getEquipment() != null
                                    && entry.getKey().equals(loan.getEquipment().getId()))
                            .findFirst()
                            .orElseThrow();
                    return new ReportSummaryDTO.TopEquipmentDTO(
                            entry.getKey(),
                            sample.getEquipment().getDeviceType(),
                            sample.getEquipment().getSerialNumber(),
                            entry.getValue());
                })
                .toList();

        return new ReportSummaryDTO(
                equipmentRepository.count(),
                equipmentByStatus,
                reservationsByStatus,
                loansActive,
                loansOverdue,
                loansReturned,
                defectsByStatus,
                topEquipment);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public String exportLoansCsv() {
        StringBuilder csv = new StringBuilder(
                "id;user;deviceType;serialNumber;borrowDate;expectedReturnDate;actualReturnDate;status\n");

        for (LoanEntity loan : loanRepository.findAllByOrderByBorrowDateDesc()) {
            String status = loan.getActualReturnDate() != null ? "ZWROCONY"
                    : (loan.getExpectedReturnDate() != null
                            && loan.getExpectedReturnDate().isBefore(LocalDateTime.now()) ? "PO_TERMINIE" : "AKTYWNY");
            csv.append(loan.getId()).append(';')
                    .append(cell(loan.getUser() != null ? loan.getUser().getEmail() : "")).append(';')
                    .append(cell(loan.getEquipment() != null ? loan.getEquipment().getDeviceType() : "")).append(';')
                    .append(cell(loan.getEquipment() != null ? loan.getEquipment().getSerialNumber() : "")).append(';')
                    .append(cell(loan.getBorrowDate() != null ? loan.getBorrowDate().toString() : "")).append(';')
                    .append(cell(loan.getExpectedReturnDate() != null ? loan.getExpectedReturnDate().toString() : ""))
                    .append(';')
                    .append(cell(loan.getActualReturnDate() != null ? loan.getActualReturnDate().toString() : ""))
                    .append(';')
                    .append(status).append('\n');
        }

        return csv.toString();
    }

    private String cell(String value) {
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
