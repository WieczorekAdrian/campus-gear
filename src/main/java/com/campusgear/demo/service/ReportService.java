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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
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

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public byte[] exportLoansPdf() {
        List<LoanEntity> loans = loanRepository.findAllByOrderByBorrowDateDesc();

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = newPage(document);
            PDPageContentStream content = newContent(document, page);
            float y = startY();

            y = line(content, y, "Campus Gear - wypozyczenia", PDType1Font.HELVETICA_BOLD, 14);
            y = line(content, y, "Wygenerowano: " + LocalDateTime.now(), PDType1Font.HELVETICA, 9);
            y -= 8;
            y = line(content, y, "ID | User | Sprzet | S/N | Wydano | Termin | Zwrocono | Status",
                    PDType1Font.HELVETICA_BOLD, 9);

            for (LoanEntity loan : loans) {
                if (y < 60) {
                    content.close();
                    page = newPage(document);
                    content = newContent(document, page);
                    y = startY();
                }
                String status = loan.getActualReturnDate() != null ? "ZWROCONY"
                        : (loan.getExpectedReturnDate() != null
                                && loan.getExpectedReturnDate().isBefore(LocalDateTime.now()) ? "PO_TERMINIE"
                                : "AKTYWNY");
                String row = loan.getId() + " | "
                        + cut(loan.getUser() != null ? loan.getUser().getEmail() : "", 24) + " | "
                        + cut(loan.getEquipment() != null ? loan.getEquipment().getDeviceType() : "", 20) + " | "
                        + cut(loan.getEquipment() != null ? loan.getEquipment().getSerialNumber() : "", 14) + " | "
                        + date(loan.getBorrowDate()) + " | "
                        + date(loan.getExpectedReturnDate()) + " | "
                        + date(loan.getActualReturnDate()) + " | "
                        + status;
                y = line(content, y, row, PDType1Font.HELVETICA, 8);
            }

            content.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Nie udało się wygenerować PDF", ex);
        }
    }

    private static final PDRectangle LANDSCAPE_A4 =
            new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

    private PDPage newPage(PDDocument document) {
        PDPage page = new PDPage(LANDSCAPE_A4);
        document.addPage(page);
        return page;
    }

    private PDPageContentStream newContent(PDDocument document, PDPage page) throws IOException {
        return new PDPageContentStream(document, page);
    }

    private float startY() {
        return LANDSCAPE_A4.getHeight() - 40;
    }

    private float line(PDPageContentStream content, float y, String text,
                       PDType1Font font, int size) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(40, y);
        content.showText(ascii(text));
        content.endText();
        return y - (size + 6);
    }

    private String date(LocalDateTime value) {
        return value != null ? value.toString().substring(0, 10) : "-";
    }

    private String cut(String value, int max) {
        String clean = ascii(value);
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    /**
     * Wbudowany Helvetica (WinAnsi) nie ma polskich znaków, a nie bundlujemy
     * zewnętrznego fontu - więc transliterujemy (ą->a itd.). Świadomy kompromis.
     */
    private String ascii(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replace('ł', 'l').replace('Ł', 'L');
    }
}
