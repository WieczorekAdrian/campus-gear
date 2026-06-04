package com.campusgear.demo.service;

import com.campusgear.demo.dto.ReservationRequestDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.status.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationEntityRepository reservationRepository;
    private final EquipmentEntityRepository equipmentRepository;

    public void createReservation(ReservationRequestDTO dto, UserEntity user) {
        // 1. Walidacja czasów - PIERWSZA! (nie wymaga bazy danych)
        if (dto.startDate().isAfter(dto.endDate())) {
            throw new RuntimeException("Data startu nie może być po dacie końca!");
        }

        // 2. Dopiero teraz szukamy sprzętu
        EquipmentEntity equipment = equipmentRepository.findById(dto.equipmentId())
                .orElseThrow(() -> new RuntimeException("Sprzęt o ID " + dto.equipmentId() + " nie istnieje!"));

        // 3. Walidacja zajętości
        boolean isOccupied = reservationRepository.existsByEquipmentIdAndStartDateLessThanAndEndDateGreaterThan(
                dto.equipmentId(), dto.endDate(), dto.startDate()
        );

        if (isOccupied) {
            throw new RuntimeException("Ten sprzęt jest już zarezerwowany w tym terminie.");
        }

        // 3. Zapis
        ReservationEntity reservation = new ReservationEntity();
        reservation.setUser(user);
        // Pobierz encję sprzętu...
        reservation.setStartDate(dto.startDate());
        reservation.setEndDate(dto.endDate());
        reservation.setStatus(ReservationStatus.AKTYWNA);

        reservation.setEquipment(equipment);

        reservationRepository.save(reservation);
    }
}