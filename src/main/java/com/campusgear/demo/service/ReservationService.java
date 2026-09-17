package com.campusgear.demo.service;

import com.campusgear.demo.dto.ReservationRequestDTO;
import com.campusgear.demo.dto.ReservationResponseDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.exception.ReservationConflictException;
import com.campusgear.demo.exception.ResourceNotFoundException;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.status.ReservationStatus;
import com.campusgear.demo.status.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationEntityRepository reservationRepository;
    private final EquipmentEntityRepository equipmentRepository;
    private final UserEntityRepository userRepository;

    @Transactional
    public ReservationResponseDTO createReservation(ReservationRequestDTO dto, String email) {
        log.info("Attempting to create reservation for user {} on equipment {}", email, dto.equipmentId());

        if (dto.startDate().isAfter(dto.endDate())) {
            throw new IllegalArgumentException("Data startu nie może być po dacie końca!");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));
        log.debug("User found with id {} and email {}", user.getId(), user.getEmail());

        EquipmentEntity equipment = equipmentRepository.findAndLockById(dto.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Sprzęt o ID " + dto.equipmentId() + " nie istnieje!"));
        log.debug("Equipment found with id {} and type {}", equipment.getId(), equipment.getDeviceType());

        long daysRequested = ChronoUnit.DAYS.between(dto.startDate(), dto.endDate());

        if (equipment.getMaxRentalDays() != null && daysRequested > equipment.getMaxRentalDays()) {
            throw new ReservationConflictException("Nie można wypożyczyć sprzętu na dłużej niż "
                    + equipment.getMaxRentalDays() + " dni.");
        }

        boolean isOccupied = reservationRepository
                .existsByEquipmentIdAndStartDateLessThanAndEndDateGreaterThanAndStatusIn(
                        dto.equipmentId(), dto.endDate(), dto.startDate(),
                        java.util.List.of(ReservationStatus.AKTYWNA, ReservationStatus.WYPOZYCZONA));

        if (isOccupied) {
            throw new ReservationConflictException("Ten sprzęt jest już zarezerwowany w tym terminie.");
        }

        ReservationEntity reservation = new ReservationEntity();
        reservation.setUser(userRepository.getReferenceById(user.getId()));
        reservation.setEquipment(equipment);
        reservation.setStartDate(dto.startDate());
        reservation.setEndDate(dto.endDate());
        reservation.setStatus(ReservationStatus.AKTYWNA);

        ReservationEntity saved = reservationRepository.save(reservation);
        log.info("Successfully created reservation id {} for user {}", saved.getId(), email);

        return toDto(saved);
    }

    @Transactional
    public ReservationResponseDTO cancelReservation(Long reservationId, String email) {
        log.info("Attempting to cancel reservation {} by user {}", reservationId, email);

        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono rezerwacji o ID: " + reservationId));

        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        boolean isOwner = reservation.getUser().getId().equals(caller.getId());
        boolean isOpiekun = caller.getRole() == Role.ROLE_OPIEKUN || caller.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isOpiekun) {
            throw new AccessDeniedException("Brak uprawnień do anulowania tej rezerwacji.");
        }

        if (reservation.getStatus() != ReservationStatus.AKTYWNA) {
            throw new ReservationConflictException("Można anulować tylko aktywną rezerwację.");
        }

        if (!reservation.getStartDate().isAfter(LocalDateTime.now())) {
            throw new ReservationConflictException("Można anulować tylko rezerwację przed jej rozpoczęciem.");
        }

        reservation.setStatus(ReservationStatus.ANULOWANA);
        ReservationEntity saved = reservationRepository.save(reservation);
        log.info("Successfully cancelled reservation id {} by user {}", saved.getId(), email);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getMyReservations(String email, ReservationStatus status) {
        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        List<ReservationEntity> reservations = (status == null)
                ? reservationRepository.findByUser_EmailOrderByStartDateDesc(caller.getEmail())
                : reservationRepository.findByUser_EmailAndStatusOrderByStartDateDesc(caller.getEmail(), status);

        return reservations.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getAllReservations(ReservationStatus status) {
        List<ReservationEntity> reservations = (status == null)
                ? reservationRepository.findAllByOrderByStartDateDesc()
                : reservationRepository.findByStatusOrderByStartDateDesc(status);

        return reservations.stream().map(this::toDto).toList();
    }

    private ReservationResponseDTO toDto(ReservationEntity reservation) {
        EquipmentEntity equipment = reservation.getEquipment();
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
        return new ReservationResponseDTO(
                reservation.getId(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getStatus(),
                reservation.getUser() != null ? reservation.getUser().getEmail() : null,
                equipmentDto
        );
    }
}
