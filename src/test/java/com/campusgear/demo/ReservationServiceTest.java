package com.campusgear.demo;

import com.campusgear.demo.dto.ReservationRequestDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.service.ReservationService;
import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.status.ReservationStatus;
import com.campusgear.demo.status.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationEntityRepository reservationRepository;

    @Mock
    private EquipmentEntityRepository equipmentRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void shouldCreateReservationSuccessfully() {
        // Given
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        ReservationRequestDTO dto = new ReservationRequestDTO(1L, start, end);
        UserEntity user = new UserEntity();
        EquipmentEntity equipment = new EquipmentEntity();

        when(equipmentRepository.findAndLockById(1L)).thenReturn(Optional.of(equipment));
        when(reservationRepository.existsByEquipmentIdAndStartDateLessThanAndEndDateGreaterThan(any(), any(), any()))
                .thenReturn(false);

        // When
        reservationService.createReservation(dto, user);

        // Then
        verify(reservationRepository, times(1)).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDatesAreInvalid() {
        // Given
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = LocalDateTime.now().plusDays(1); // Koniec przed startem!
        ReservationRequestDTO dto = new ReservationRequestDTO(1L, start, end);

        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(dto, new UserEntity()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Data startu nie może być po dacie końca!");
    }

    @Test
    void shouldThrowExceptionWhenEquipmentIsOccupied() {
        // Given
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        ReservationRequestDTO dto = new ReservationRequestDTO(1L, start, end);

        when(equipmentRepository.findAndLockById(1L)).thenReturn(Optional.of(new EquipmentEntity()));
        when(reservationRepository.existsByEquipmentIdAndStartDateLessThanAndEndDateGreaterThan(any(), any(), any()))
                .thenReturn(true); // Sprzęt zajęty!

        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(dto, new UserEntity()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ten sprzęt jest już zarezerwowany w tym terminie.");
    }

    @Test
    void shouldThrowExceptionWhenRentalExceedsLimit() {
        // Given
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setMaxRentalDays(3); // Limit na 3 dni

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(5); // Próba na 5 dni
        ReservationRequestDTO dto = new ReservationRequestDTO(1L, start, end);

        when(equipmentRepository.findAndLockById(1L)).thenReturn(Optional.of(equipment));

        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(dto, new UserEntity()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Nie można wypożyczyć sprzętu na dłużej niż 3 dni.");
    }

    @Test
    void shouldCancelReservationSuccessfullyByOwner() {
        // Given
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        owner.setRole(Role.ROLE_STUDENT);

        ReservationEntity reservation = new ReservationEntity();
        reservation.setId(10L);
        reservation.setUser(owner);
        reservation.setStartDate(LocalDateTime.now().plusDays(1));
        reservation.setStatus(ReservationStatus.AKTYWNA);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        // When
        reservationService.cancelReservation(10L, owner);

        // Then
        verify(reservationRepository, times(1)).save(reservation);
        org.assertj.core.api.Assertions.assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ANULOWANA);
    }

    @Test
    void shouldCancelReservationSuccessfullyByOpiekun() {
        // Given
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        owner.setRole(Role.ROLE_STUDENT);

        UserEntity opiekun = new UserEntity();
        opiekun.setId(2L);
        opiekun.setRole(Role.ROLE_OPIEKUN);

        ReservationEntity reservation = new ReservationEntity();
        reservation.setId(10L);
        reservation.setUser(owner);
        reservation.setStartDate(LocalDateTime.now().plusDays(1));
        reservation.setStatus(ReservationStatus.AKTYWNA);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        // When
        reservationService.cancelReservation(10L, opiekun);

        // Then
        verify(reservationRepository, times(1)).save(reservation);
        org.assertj.core.api.Assertions.assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ANULOWANA);
    }

    @Test
    void shouldThrowExceptionWhenCancellingByNonOwner() {
        // Given
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        owner.setRole(Role.ROLE_STUDENT);

        UserEntity otherUser = new UserEntity();
        otherUser.setId(2L);
        otherUser.setRole(Role.ROLE_STUDENT);

        ReservationEntity reservation = new ReservationEntity();
        reservation.setId(10L);
        reservation.setUser(owner);
        reservation.setStartDate(LocalDateTime.now().plusDays(1));
        reservation.setStatus(ReservationStatus.AKTYWNA);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        // When & Then
        assertThatThrownBy(() -> reservationService.cancelReservation(10L, otherUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nie masz uprawnień do anulowania tej rezerwacji!");
    }

    @Test
    void shouldThrowExceptionWhenCancellingAfterStartDate() {
        // Given
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        owner.setRole(Role.ROLE_STUDENT);

        ReservationEntity reservation = new ReservationEntity();
        reservation.setId(10L);
        reservation.setUser(owner);
        reservation.setStartDate(LocalDateTime.now().minusDays(1)); // Rozpoczęła się wczoraj!
        reservation.setStatus(ReservationStatus.AKTYWNA);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        // When & Then
        assertThatThrownBy(() -> reservationService.cancelReservation(10L, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nie można anulować rezerwacji po jej rozpoczęciu!");
    }

    @Test
    void shouldThrowExceptionWhenReservationAlreadyCancelledOrFinished() {
        // Given
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        owner.setRole(Role.ROLE_STUDENT);

        ReservationEntity cancelledReservation = new ReservationEntity();
        cancelledReservation.setId(10L);
        cancelledReservation.setUser(owner);
        cancelledReservation.setStartDate(LocalDateTime.now().plusDays(1));
        cancelledReservation.setStatus(ReservationStatus.ANULOWANA);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(cancelledReservation));

        // When & Then
        assertThatThrownBy(() -> reservationService.cancelReservation(10L, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ta rezerwacja jest już anulowana!");
    }
}