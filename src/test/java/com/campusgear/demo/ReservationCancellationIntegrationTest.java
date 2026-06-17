package com.campusgear.demo;

import com.campusgear.demo.dto.LoginDto;
import com.campusgear.demo.dto.RegisterDto;
import com.campusgear.demo.dto.ReservationRequestDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.status.EquipmentStatus;
import com.campusgear.demo.status.ReservationStatus;
import com.campusgear.demo.status.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class ReservationCancellationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private EquipmentEntityRepository equipmentRepository;

    @Autowired
    private ReservationEntityRepository reservationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserEntity testStudent;
    private UserEntity testOpiekun;
    private EquipmentEntity testEquipment;

    @BeforeEach
    void setUp() throws Exception {
        reservationRepository.deleteAll();
        equipmentRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Rejestracja studenta
        RegisterDto studentRegister = new RegisterDto(
                "student@campus.edu.pl",
                "Password123!",
                "Jan",
                "Kowalski"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentRegister)))
                .andExpect(status().isCreated());

        testStudent = userRepository.findByEmail("student@campus.edu.pl").orElseThrow();

        // 2. Rejestracja i ręczne podbicie do roli opiekuna
        RegisterDto opiekunRegister = new RegisterDto(
                "opiekun@campus.edu.pl",
                "Password123!",
                "Anna",
                "Nowak"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(opiekunRegister)))
                .andExpect(status().isCreated());

        testOpiekun = userRepository.findByEmail("opiekun@campus.edu.pl").orElseThrow();
        testOpiekun.setRole(Role.ROLE_OPIEKUN);
        testOpiekun = userRepository.save(testOpiekun);

        // 3. Stworzenie sprzętu
        testEquipment = new EquipmentEntity();
        testEquipment.setDeviceType("Projektor");
        testEquipment.setSerialNumber("PROJ-998");
        testEquipment.setStatus(EquipmentStatus.DOSTEPNY);
        testEquipment.setAcademicAccount(false);
        testEquipment = equipmentRepository.save(testEquipment);
    }

    private String getJwtToken(String email, String password) throws Exception {
        LoginDto loginDto = new LoginDto(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("token").asText();
    }

    @Test
    void shouldCancelReservationSuccessfullyByOwner() throws Exception {
        // Given
        String studentToken = getJwtToken("student@campus.edu.pl", "Password123!");

        // Tworzymy rezerwację dla studenta (w przyszłości)
        ReservationEntity reservation = new ReservationEntity();
        reservation.setUser(testStudent);
        reservation.setEquipment(testEquipment);
        reservation.setStartDate(LocalDateTime.now().plusDays(2));
        reservation.setEndDate(LocalDateTime.now().plusDays(4));
        reservation.setStatus(ReservationStatus.AKTYWNA);
        reservation = reservationRepository.save(reservation);

        // When & Then
        mockMvc.perform(post("/api/reservations/" + reservation.getId() + "/cancel")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        ReservationEntity updated = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.ANULOWANA);
    }

    @Test
    void shouldCancelReservationSuccessfullyByOpiekun() throws Exception {
        // Given
        String opiekunToken = getJwtToken("opiekun@campus.edu.pl", "Password123!");

        // Rezerwacja studenta
        ReservationEntity reservation = new ReservationEntity();
        reservation.setUser(testStudent);
        reservation.setEquipment(testEquipment);
        reservation.setStartDate(LocalDateTime.now().plusDays(2));
        reservation.setEndDate(LocalDateTime.now().plusDays(4));
        reservation.setStatus(ReservationStatus.AKTYWNA);
        reservation = reservationRepository.save(reservation);

        // When & Then
        mockMvc.perform(post("/api/reservations/" + reservation.getId() + "/cancel")
                        .header("Authorization", "Bearer " + opiekunToken))
                .andExpect(status().isOk());

        ReservationEntity updated = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.ANULOWANA);
    }

    @Test
    void shouldFailWhenNonOwnerAttemptsToCancel() throws Exception {
        // Given - rejestrujemy drugiego studenta
        RegisterDto student2Register = new RegisterDto(
                "student2@campus.edu.pl",
                "Password123!",
                "Marek",
                "Kowal"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(student2Register)))
                .andExpect(status().isCreated());

        String student2Token = getJwtToken("student2@campus.edu.pl", "Password123!");

        // Rezerwacja pierwszego studenta
        ReservationEntity reservation = new ReservationEntity();
        reservation.setUser(testStudent);
        reservation.setEquipment(testEquipment);
        reservation.setStartDate(LocalDateTime.now().plusDays(2));
        reservation.setEndDate(LocalDateTime.now().plusDays(4));
        reservation.setStatus(ReservationStatus.AKTYWNA);
        reservation = reservationRepository.save(reservation);

        // When & Then
        mockMvc.perform(post("/api/reservations/" + reservation.getId() + "/cancel")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailWhenCancellingAfterStartDate() throws Exception {
        // Given
        String studentToken = getJwtToken("student@campus.edu.pl", "Password123!");

        // Rezerwacja w przeszłości (już się rozpoczęła)
        ReservationEntity reservation = new ReservationEntity();
        reservation.setUser(testStudent);
        reservation.setEquipment(testEquipment);
        reservation.setStartDate(LocalDateTime.now().minusDays(1));
        reservation.setEndDate(LocalDateTime.now().plusDays(2));
        reservation.setStatus(ReservationStatus.AKTYWNA);
        reservation = reservationRepository.save(reservation);

        // When & Then
        mockMvc.perform(post("/api/reservations/" + reservation.getId() + "/cancel")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());
    }
}
