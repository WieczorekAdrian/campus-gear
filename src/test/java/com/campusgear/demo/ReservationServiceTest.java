package com.campusgear.demo;

import com.campusgear.demo.dto.ReservationRequestDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.service.ReservationService;
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

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));
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

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(new EquipmentEntity()));
        when(reservationRepository.existsByEquipmentIdAndStartDateLessThanAndEndDateGreaterThan(any(), any(), any()))
                .thenReturn(true); // Sprzęt zajęty!

        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(dto, new UserEntity()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ten sprzęt jest już zarezerwowany w tym terminie.");
    }
}