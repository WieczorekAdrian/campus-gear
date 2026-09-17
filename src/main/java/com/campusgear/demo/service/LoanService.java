package com.campusgear.demo.service;

import com.campusgear.demo.dto.LoanResponseDTO;
import com.campusgear.demo.dto.LoanReturnDTO;
import com.campusgear.demo.dto.ReservationResponseDTO;
import com.campusgear.demo.entity.DefectReportEntity;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.LoanEntity;
import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.exception.ReservationConflictException;
import com.campusgear.demo.exception.ResourceNotFoundException;
import com.campusgear.demo.repository.DefectReportEntityRepository;
import com.campusgear.demo.repository.LoanEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.status.DefectStatus;
import com.campusgear.demo.status.EquipmentStatus;
import com.campusgear.demo.status.ReservationStatus;
import com.campusgear.demo.status.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanEntityRepository loanRepository;
    private final ReservationEntityRepository reservationRepository;
    private final UserEntityRepository userRepository;
    private final DefectReportEntityRepository defectReportRepository;

    @Transactional
    public LoanResponseDTO issueLoan(Long reservationId, String email) {
        log.info("Attempting to issue loan for reservation {} by user {}", reservationId, email);

        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono rezerwacji o ID: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.AKTYWNA) {
            throw new ReservationConflictException("Wydać można tylko aktywną rezerwację.");
        }

        if (loanRepository.existsByReservationId(reservationId)) {
            throw new ReservationConflictException("Ta rezerwacja została już zrealizowana (wydano sprzęt).");
        }

        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        boolean isOpiekun = caller.getRole() == Role.ROLE_OPIEKUN || caller.getRole() == Role.ROLE_ADMIN;

        if (!isOpiekun) {
            throw new AccessDeniedException("Sprzęt może wydać tylko opiekun.");
        }

        EquipmentEntity equipment = reservation.getEquipment();

        LoanEntity loan = new LoanEntity();
        loan.setUser(userRepository.getReferenceById(reservation.getUser().getId()));
        loan.setEquipment(equipment);
        loan.setReservation(reservation);
        loan.setBorrowDate(LocalDateTime.now());
        loan.setExpectedReturnDate(reservation.getEndDate());

        equipment.setStatus(EquipmentStatus.WYPOZYCZONY);
        reservation.setStatus(ReservationStatus.WYPOZYCZONA);

        LoanEntity saved = loanRepository.save(loan);
        log.info("Successfully issued loan id {} for reservation {}", saved.getId(), reservationId);

        return toDto(saved);
    }

    @Transactional
    public LoanResponseDTO requestReturn(Long loanId, String email) {
        log.info("Return requested for loan {} by user {}", loanId, email);

        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono wypożyczenia o ID: " + loanId));

        if (loan.getActualReturnDate() != null) {
            throw new ReservationConflictException("Ten sprzęt został już zwrócony.");
        }

        if (loan.getReturnRequestedAt() != null) {
            throw new ReservationConflictException("Zwrot został już zgłoszony, czeka na odbiór przez opiekuna.");
        }

        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        boolean isOwner = loan.getUser().getId().equals(caller.getId());
        boolean isOpiekun = caller.getRole() == Role.ROLE_OPIEKUN || caller.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isOpiekun) {
            throw new AccessDeniedException("Brak uprawnień do zgłoszenia zwrotu tego wypożyczenia.");
        }

        loan.setReturnRequestedAt(LocalDateTime.now());

        LoanEntity saved = loanRepository.save(loan);
        log.info("Return requested for loan id {} by user {}", saved.getId(), email);

        return toDto(saved);
    }

    @Transactional
    public LoanResponseDTO confirmReturn(Long loanId, String email, LoanReturnDTO dto) {
        log.info("Attempting to confirm return of loan {} by user {}", loanId, email);

        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono wypożyczenia o ID: " + loanId));

        if (loan.getActualReturnDate() != null) {
            throw new ReservationConflictException("Ten sprzęt został już zwrócony.");
        }

        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        boolean isOpiekun = caller.getRole() == Role.ROLE_OPIEKUN || caller.getRole() == Role.ROLE_ADMIN;

        if (!isOpiekun) {
            throw new AccessDeniedException("Odbiór zwrotu potwierdza tylko opiekun.");
        }

        boolean damaged = dto != null && Boolean.TRUE.equals(dto.damaged());

        if (loan.getReturnRequestedAt() == null) {
            loan.setReturnRequestedAt(LocalDateTime.now());
        }
        loan.setActualReturnDate(LocalDateTime.now());

        if (damaged) {
            loan.getEquipment().setStatus(EquipmentStatus.SERWISOWANY);

            DefectReportEntity defect = new DefectReportEntity();
            defect.setDescription(dto.damageDescription() != null && !dto.damageDescription().isBlank()
                    ? dto.damageDescription()
                    : "Uszkodzenie zgłoszone przy zwrocie");
            defect.setReportDate(LocalDateTime.now());
            defect.setStatus(DefectStatus.ZGLOSZONA);
            defect.setEquipment(loan.getEquipment());
            defect.setReporter(caller);
            defectReportRepository.save(defect);
            log.info("Defect reported for equipment {} on loan {}", loan.getEquipment().getId(), loanId);
        } else {
            loan.getEquipment().setStatus(EquipmentStatus.DOSTEPNY);
        }

        if (loan.getReservation() != null) {
            loan.getReservation().setStatus(ReservationStatus.ZAKONCZONA);
        }

        LoanEntity saved = loanRepository.save(loan);
        log.info("Successfully confirmed return of loan id {} by user {} (damaged={})", saved.getId(), email, damaged);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDTO> getMyLoans(String email) {
        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        return loanRepository.findByUser_EmailOrderByBorrowDateDesc(caller.getEmail())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDTO> getAllLoans(boolean activeOnly) {
        List<LoanEntity> loans = activeOnly
                ? loanRepository.findByActualReturnDateIsNullOrderByBorrowDateDesc()
                : loanRepository.findAllByOrderByBorrowDateDesc();

        return loans.stream().map(this::toDto).toList();
    }

    private LoanResponseDTO toDto(LoanEntity loan) {
        EquipmentEntity equipment = loan.getEquipment();
        ReservationResponseDTO.EquipmentSummaryDTO equipmentDto = null;
        if (equipment != null) {
            equipmentDto = new ReservationResponseDTO.EquipmentSummaryDTO(
                    equipment.getId(),
                    equipment.getDeviceType(),
                    equipment.getTechnicalSpecification(),
                    equipment.getSerialNumber(),
                    equipment.getLocation()
            );
        }
        return new LoanResponseDTO(
                loan.getId(),
                loan.getBorrowDate(),
                loan.getExpectedReturnDate(),
                loan.getReturnRequestedAt(),
                loan.getActualReturnDate(),
                loan.getReservation() != null ? loan.getReservation().getId() : null,
                loan.getUser() != null ? loan.getUser().getEmail() : null,
                equipmentDto
        );
    }
}
