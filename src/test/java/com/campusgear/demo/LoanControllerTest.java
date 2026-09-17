package com.campusgear.demo;

import com.campusgear.demo.dto.LoanResponseDTO;
import com.campusgear.demo.dto.ReservationResponseDTO;
import com.campusgear.demo.service.LoanService;
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
class LoanControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    private LoanResponseDTO response() {
        return new LoanResponseDTO(
                7L,
                LocalDateTime.of(2026, 10, 5, 10, 0),
                LocalDateTime.of(2026, 10, 8, 10, 0),
                null,
                null,
                3L,
                "student@campus.edu.pl",
                new ReservationResponseDTO.EquipmentSummaryDTO(
                        2L, "Laptop", "Dell XPS", "DELL-12345", "Main Hall"));
    }

    @Test
    void shouldIssueLoan() throws Exception {
        when(loanService.issueLoan(eq(3L), eq("student@campus.edu.pl"))).thenReturn(response());

        mockMvc.perform(post("/api/loans")
                        .with(user("student@campus.edu.pl").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reservationId":3}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.equipment.deviceType").value("Laptop"));
    }

    @Test
    void shouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .with(user("student@campus.edu.pl").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequestReturn() throws Exception {
        when(loanService.requestReturn(eq(7L), eq("student@campus.edu.pl"))).thenReturn(response());

        mockMvc.perform(patch("/api/loans/7/request-return")
                        .with(user("student@campus.edu.pl").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void shouldConfirmReturn() throws Exception {
        LoanResponseDTO returned = new LoanResponseDTO(
                7L, response().borrowDate(), response().expectedReturnDate(), response().returnRequestedAt(),
                LocalDateTime.of(2026, 10, 6, 10, 0), 3L, response().userEmail(), response().equipment());
        when(loanService.confirmReturn(eq(7L), eq("opiekun@campus.edu.pl"), any())).thenReturn(returned);

        mockMvc.perform(patch("/api/loans/7/confirm-return")
                        .with(user("opiekun@campus.edu.pl").roles("OPIEKUN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"damaged":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualReturnDate").exists());
    }

    @Test
    void shouldConfirmDamagedReturn() throws Exception {
        when(loanService.confirmReturn(eq(7L), eq("opiekun@campus.edu.pl"), any())).thenReturn(response());

        mockMvc.perform(patch("/api/loans/7/confirm-return")
                        .with(user("opiekun@campus.edu.pl").roles("OPIEKUN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"damaged":true,"damageDescription":"Pęknięta obudowa"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void shouldForbidConfirmForStudent() throws Exception {
        mockMvc.perform(patch("/api/loans/7/confirm-return")
                        .with(user("student@campus.edu.pl").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"damaged":false}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnMyLoans() throws Exception {
        when(loanService.getMyLoans(eq("student@campus.edu.pl"))).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/loans/mine")
                        .with(user("student@campus.edu.pl").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].equipment.serialNumber").value("DELL-12345"));
    }

    @Test
    void shouldRequireAuthForLoans() throws Exception {
        mockMvc.perform(get("/api/loans/mine"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidAllLoansForStudent() throws Exception {
        mockMvc.perform(get("/api/loans")
                        .with(user("student@campus.edu.pl").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnAllLoansForOpiekun() throws Exception {
        when(loanService.getAllLoans(true)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/loans")
                        .with(user("opiekun@campus.edu.pl").roles("OPIEKUN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value("student@campus.edu.pl"));
    }
}
