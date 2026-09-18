package com.campusgear.demo.service;

import com.campusgear.demo.dto.LoanResponseDTO;
import com.campusgear.demo.dto.LoanReturnDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.LoanEntity;
import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.exception.ReservationConflictException;
import com.campusgear.demo.exception.ResourceNotFoundException;
import com.campusgear.demo.mapper.LoanMapper;
import com.campusgear.demo.repository.LoanEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.status.EquipmentStatus;
import com.campusgear.demo.status.ReservationStatus;
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
public class LoanService {

    private final LoanEntityRepository loanRepository;
    private final ReservationEntityRepository reservationRepository;
    private final UserEntityRepository userRepository;
    private final DefectReportService defectReportService;
    private final LoanMapper loanMapper;

    @Transactional
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
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
        log.debug("Issued by {} with role {}", caller.getEmail(), caller.getRole());

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

        return loanMapper.toDto(saved);
    }

    @Transactional
    @PreAuthorize("@loanAccess.canRequestReturn(#loanId, authentication.name)")
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

        // Encja jest managed w transakcji - dirty checking sam zapisze zmianę przy commicie.
        loan.setReturnRequestedAt(LocalDateTime.now());
        log.info("Return requested for loan id {} by user {}", loan.getId(), email);

        return loanMapper.toDto(loan);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public LoanResponseDTO confirmReturn(Long loanId, String email, LoanReturnDTO dto) {
        log.info("Attempting to confirm return of loan {} by user {}", loanId, email);

        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono wypożyczenia o ID: " + loanId));

        if (loan.getActualReturnDate() != null) {
            throw new ReservationConflictException("Ten sprzęt został już zwrócony.");
        }

        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        boolean damaged = dto != null && Boolean.TRUE.equals(dto.damaged());

        if (loan.getReturnRequestedAt() == null) {
            loan.setReturnRequestedAt(LocalDateTime.now());
        }
        loan.setActualReturnDate(LocalDateTime.now());

        if (damaged) {
            loan.getEquipment().setStatus(EquipmentStatus.SERWISOWANY);
            defectReportService.reportDefect(loan.getEquipment(), caller, dto.damageDescription());
            log.info("Defect reported for equipment {} on loan {}", loan.getEquipment().getId(), loanId);
        } else {
            loan.getEquipment().setStatus(EquipmentStatus.DOSTEPNY);
        }

        if (loan.getReservation() != null) {
            loan.getReservation().setStatus(ReservationStatus.ZAKONCZONA);
        }

        log.info("Successfully confirmed return of loan id {} by user {} (damaged={})", loan.getId(), email, damaged);

        return loanMapper.toDto(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDTO> getMyLoans(String email) {
        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        return loanRepository.findByUser_EmailOrderByBorrowDateDesc(caller.getEmail())
                .stream()
                .map(loanMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDTO> getAllLoans(boolean activeOnly) {
        List<LoanEntity> loans = activeOnly
                ? loanRepository.findByActualReturnDateIsNullOrderByBorrowDateDesc()
                : loanRepository.findAllByOrderByBorrowDateDesc();

        return loans.stream().map(loanMapper::toDto).toList();
    }
}
