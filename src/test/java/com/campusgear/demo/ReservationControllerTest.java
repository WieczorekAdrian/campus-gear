package com.campusgear.demo;

import com.campusgear.demo.dto.ReservationResponseDTO;
import com.campusgear.demo.service.ReservationService;
import com.campusgear.demo.status.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    private ReservationResponseDTO response() {
        return new ReservationResponseDTO(
                1L,
                LocalDateTime.of(2026, 10, 1, 10, 0),
                LocalDateTime.of(2026, 10, 3, 10, 0),
                ReservationStatus.AKTYWNA,
                new ReservationResponseDTO.EquipmentSummaryDTO(
                        2L, "Laptop", "Dell XPS", "DELL-12345", "Main Hall"));
    }

    @Test
    void shouldCreateReservation() throws Exception {
        when(reservationService.createReservation(any(), eq("student@campus.edu.pl")))
                .thenReturn(response());

        mockMvc.perform(post("/api/reservations")
                        .with(user("student@campus.edu.pl").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"equipmentId":2,"startDate":"2026-10-01T10:00:00","endDate":"2026-10-03T10:00:00"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("AKTYWNA"))
                .andExpect(jsonPath("$.equipment.deviceType").value("Laptop"));
    }

    @Test
    void shouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(user("student@campus.edu.pl").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-10-01T10:00:00","endDate":"2026-10-03T10:00:00"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCancelReservation() throws Exception {
        ReservationResponseDTO cancelled = new ReservationResponseDTO(
                1L, response().startDate(), response().endDate(),
                ReservationStatus.ANULOWANA, response().equipment());
        when(reservationService.cancelReservation(eq(1L), eq("student@campus.edu.pl")))
                .thenReturn(cancelled);

        mockMvc.perform(patch("/api/reservations/1/cancel")
                        .with(user("student@campus.edu.pl").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANULOWANA"));
    }

    @Test
    void shouldReturnMyReservations() throws Exception {
        when(reservationService.getMyReservations(eq("student@campus.edu.pl"), any()))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/reservations/mine")
                        .with(user("student@campus.edu.pl").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].equipment.serialNumber").value("DELL-12345"));
    }

    @Test
    void shouldRequireAuthForReservations() throws Exception {
        mockMvc.perform(get("/api/reservations/mine"))
                .andExpect(status().isForbidden());
    }
}
