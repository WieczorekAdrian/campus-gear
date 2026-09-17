package com.campusgear.demo;

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
import com.campusgear.demo.service.ReservationService;
import com.campusgear.demo.status.ReservationStatus;
import com.campusgear.demo.status.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationEntityRepository reservationRepository;

    @Mock
    private EquipmentEntityRepository equipmentRepository;

    @Mock
    private UserEntityRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    private UserEntity user(String email, Long id, Role role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private EquipmentEntity equipment(Long id) {
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setId(id);
        equipment.setDeviceType("Laptop");
        equipment.setTechnicalSpecification("Dell XPS");
        equipment.setSerialNumber("DELL-12345");
        equipment.setLocation("Main Hall");
        return equipment;
    }

    @Test
    void shouldCreateReservationSuccessfully() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        ReservationRequestDTO dto = new ReservationRequestDTO(1L, start, end);
        UserEntity user = user("a@campus.edu.pl", 10L, Role.ROLE_STUDENT);
        EquipmentEntity equipment = equipment(1L);

        when(userRepository.findByEmail("a@campus.edu.pl")).thenReturn(Optional.of(user));
        when(equipmentRepository.findAndLockById(1L)).thenReturn(Optional.of(equipment));
        when(reservationRepository.existsByEquipmentIdAndStartDateLessThanAndEndDateGreaterThanAndStatusIn(
                anyLong(), any(), any(), any())).thenReturn(false);
        when(userRepository.getReferenceById(10L)).thenReturn(user);
        when(reservationRepository.save(any())).thenAnswer(inv -> {
            ReservationEntity r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        ReservationResponseDTO result = reservationService.createReservation(dto, "a@campus.edu.pl");

        assertThat(result.id()).isEqualTo(99L);
        assertThat(result.status()).isEqualTo(ReservationStatus.AKTYWNA);
        assertThat(result.equipment().deviceType()).isEqualTo("Laptop");
    }

    @Test
    void shouldThrowWhenDatesAreInvalid() {
        ReservationRequestDTO dto = new ReservationRequestDTO(
                1L, LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> reservationService.createReservation(dto, "a@campus.edu.pl"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Data startu nie może być po dacie końca!");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        ReservationRequestDTO dto = new ReservationRequestDTO(
                1L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(dto, "nope@campus.edu.pl"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowWhenEquipmentNotFound() {
        ReservationRequestDTO dto = new ReservationRequestDTO(
                1L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user("a@campus.edu.pl", 10L, Role.ROLE_STUDENT)));
        when(equipmentRepository.findAndLockById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(dto, "a@campus.edu.pl"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sprzęt o ID 1 nie istnieje!");
    }

    @Test
    void shouldThrowWhenEquipmentIsOccupied() {
        ReservationRequestDTO dto = new ReservationRequestDTO(
                1L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user("a@campus.edu.pl", 10L, Role.ROLE_STUDENT)));
        when(equipmentRepository.findAndLockById(1L)).thenReturn(Optional.of(equipment(1L)));
        when(reservationRepository.existsByEquipmentIdAndStartDateLessThanAndEndDateGreaterThanAndStatusIn(
                anyLong(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(dto, "a@campus.edu.pl"))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessage("Ten sprzęt jest już zarezerwowany w tym terminie.");
    }

    @Test
    void shouldThrowWhenRentalExceedsLimit() {
        EquipmentEntity eq = equipment(1L);
        eq.setMaxRentalDays(3);
        ReservationRequestDTO dto = new ReservationRequestDTO(
                1L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(6));
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user("a@campus.edu.pl", 10L, Role.ROLE_STUDENT)));
        when(equipmentRepository.findAndLockById(1L)).thenReturn(Optional.of(eq));

        assertThatThrownBy(() -> reservationService.createReservation(dto, "a@campus.edu.pl"))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("Nie można wypożyczyć sprzętu na dłużej niż 3 dni.");
    }

    private ReservationEntity activeReservation(Long id, Long ownerId, LocalDateTime start, LocalDateTime end) {
        ReservationEntity r = new ReservationEntity();
        r.setId(id);
        r.setStartDate(start);
        r.setEndDate(end);
        r.setStatus(ReservationStatus.AKTYWNA);
        r.setUser(user("owner@campus.edu.pl", ownerId, Role.ROLE_STUDENT));
        r.setEquipment(equipment(1L));
        return r;
    }

    @Test
    void shouldCancelOwnReservation() {
        ReservationEntity r = activeReservation(
                5L, 10L, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("owner@campus.edu.pl"))
                .thenReturn(Optional.of(user("owner@campus.edu.pl", 10L, Role.ROLE_STUDENT)));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponseDTO result = reservationService.cancelReservation(5L, "owner@campus.edu.pl");

        assertThat(result.status()).isEqualTo(ReservationStatus.ANULOWANA);
    }

    @Test
    void shouldLetOpiekunCancelForeignReservation() {
        ReservationEntity r = activeReservation(
                5L, 10L, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("opiekun@campus.edu.pl"))
                .thenReturn(Optional.of(user("opiekun@campus.edu.pl", 20L, Role.ROLE_OPIEKUN)));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponseDTO result = reservationService.cancelReservation(5L, "opiekun@campus.edu.pl");

        assertThat(result.status()).isEqualTo(ReservationStatus.ANULOWANA);
    }

    @Test
    void shouldForbidStudentCancellingForeignReservation() {
        ReservationEntity r = activeReservation(
                5L, 10L, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("other@campus.edu.pl"))
                .thenReturn(Optional.of(user("other@campus.edu.pl", 30L, Role.ROLE_STUDENT)));

        assertThatThrownBy(() -> reservationService.cancelReservation(5L, "other@campus.edu.pl"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldRejectCancellingNonActiveReservation() {
        ReservationEntity r = activeReservation(
                5L, 10L, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3));
        r.setStatus(ReservationStatus.ANULOWANA);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("owner@campus.edu.pl"))
                .thenReturn(Optional.of(user("owner@campus.edu.pl", 10L, Role.ROLE_STUDENT)));

        assertThatThrownBy(() -> reservationService.cancelReservation(5L, "owner@campus.edu.pl"))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("tylko aktywną");
    }

    @Test
    void shouldRejectCancellingStartedReservation() {
        ReservationEntity r = activeReservation(
                5L, 10L, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        when(userRepository.findByEmail("owner@campus.edu.pl"))
                .thenReturn(Optional.of(user("owner@campus.edu.pl", 10L, Role.ROLE_STUDENT)));

        assertThatThrownBy(() -> reservationService.cancelReservation(5L, "owner@campus.edu.pl"))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("przed jej rozpoczęciem");
    }

    @Test
    void shouldReturnMyReservations() {
        UserEntity caller = user("a@campus.edu.pl", 10L, Role.ROLE_STUDENT);
        when(userRepository.findByEmail("a@campus.edu.pl")).thenReturn(Optional.of(caller));
        ReservationEntity r = activeReservation(
                7L, 10L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        when(reservationRepository.findByUser_EmailOrderByStartDateDesc("a@campus.edu.pl"))
                .thenReturn(List.of(r));

        List<ReservationResponseDTO> result =
                reservationService.getMyReservations("a@campus.edu.pl", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(7L);
        verify(reservationRepository).findByUser_EmailOrderByStartDateDesc("a@campus.edu.pl");
    }

    @Test
    void shouldReturnMyReservationsFilteredByStatus() {
        UserEntity caller = user("a@campus.edu.pl", 10L, Role.ROLE_STUDENT);
        when(userRepository.findByEmail("a@campus.edu.pl")).thenReturn(Optional.of(caller));
        when(reservationRepository.findByUser_EmailAndStatusOrderByStartDateDesc(
                "a@campus.edu.pl", ReservationStatus.AKTYWNA)).thenReturn(List.of());

        List<ReservationResponseDTO> result =
                reservationService.getMyReservations("a@campus.edu.pl", ReservationStatus.AKTYWNA);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnAllReservationsForPanel() {
        ReservationEntity r = activeReservation(
                7L, 10L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        when(reservationRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(r));

        List<ReservationResponseDTO> result = reservationService.getAllReservations(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userEmail()).isEqualTo("owner@campus.edu.pl");
    }
}
