package com.campusgear.demo;

import com.campusgear.demo.dto.DefectResponseDTO;
import com.campusgear.demo.dto.ReservationResponseDTO;
import com.campusgear.demo.service.DefectReportService;
import com.campusgear.demo.status.DefectStatus;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DefectControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DefectReportService defectReportService;

    private DefectResponseDTO response() {
        return new DefectResponseDTO(
                9L,
                "Pęknięta obudowa",
                LocalDateTime.of(2026, 10, 6, 12, 0),
                DefectStatus.ZGLOSZONA,
                "student@campus.edu.pl",
                new ReservationResponseDTO.EquipmentSummaryDTO(
                        2L, "Laptop", "Dell XPS", "DELL-12345", "Main Hall"));
    }

    @Test
    void shouldForbidDefectsForStudent() throws Exception {
        mockMvc.perform(get("/api/defects")
                        .with(user("student@campus.edu.pl").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnDefectsForOpiekun() throws Exception {
        when(defectReportService.getAllDefects(any())).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/defects")
                        .with(user("opiekun@campus.edu.pl").roles("OPIEKUN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ZGLOSZONA"))
                .andExpect(jsonPath("$[0].reporterEmail").value("student@campus.edu.pl"));
    }

    @Test
    void shouldUpdateStatus() throws Exception {
        DefectResponseDTO repaired = new DefectResponseDTO(
                9L, response().description(), response().reportDate(),
                DefectStatus.NAPRAWIONA, response().reporterEmail(), response().equipment());
        when(defectReportService.updateStatus(eq(9L), eq(DefectStatus.NAPRAWIONA), eq("opiekun@campus.edu.pl")))
                .thenReturn(repaired);

        mockMvc.perform(patch("/api/defects/9/status")
                        .with(user("opiekun@campus.edu.pl").roles("OPIEKUN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"NAPRAWIONA"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NAPRAWIONA"));
    }

    @Test
    void shouldRejectEmptyStatus() throws Exception {
        mockMvc.perform(patch("/api/defects/9/status")
                        .with(user("opiekun@campus.edu.pl").roles("OPIEKUN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
