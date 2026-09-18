package com.campusgear.demo.service;

import com.campusgear.demo.dto.ReservationRequestDTO;
import com.campusgear.demo.dto.ReservationResponseDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.exception.ReservationConflictException;
import com.campusgear.demo.exception.ResourceNotFoundException;
import com.campusgear.demo.mapper.ReservationMapper;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.status.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ReservationMapper reservationMapper;

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
                        List.of(ReservationStatus.AKTYWNA, ReservationStatus.WYPOZYCZONA));

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

        return reservationMapper.toDto(saved);
    }

    @Transactional
    @PreAuthorize("@reservationAccess.canCancel(#reservationId, authentication.name)")
    public ReservationResponseDTO cancelReservation(Long reservationId, String email) {
        log.info("Attempting to cancel reservation {} by user {}", reservationId, email);

        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono rezerwacji o ID: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.AKTYWNA) {
            throw new ReservationConflictException("Można anulować tylko aktywną rezerwację.");
        }

        if (!reservation.getStartDate().isAfter(LocalDateTime.now())) {
            throw new ReservationConflictException("Można anulować tylko rezerwację przed jej rozpoczęciem.");
        }

        // Encja jest managed w transakcji - dirty checking sam zapisze zmianę przy commicie.
        reservation.setStatus(ReservationStatus.ANULOWANA);
        log.info("Successfully cancelled reservation id {} by user {}", reservation.getId(), email);

        return reservationMapper.toDto(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getMyReservations(String email, ReservationStatus status) {
        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        List<ReservationEntity> reservations = (status == null)
                ? reservationRepository.findByUser_EmailOrderByStartDateDesc(caller.getEmail())
                : reservationRepository.findByUser_EmailAndStatusOrderByStartDateDesc(caller.getEmail(), status);

        return reservations.stream().map(reservationMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getAllReservations(ReservationStatus status) {
        List<ReservationEntity> reservations = (status == null)
                ? reservationRepository.findAllByOrderByStartDateDesc()
                : reservationRepository.findByStatusOrderByStartDateDesc(status);

        return reservations.stream().map(reservationMapper::toDto).toList();
    }
}
