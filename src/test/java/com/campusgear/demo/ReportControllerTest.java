package com.campusgear.demo;

import com.campusgear.demo.dto.ReportSummaryDTO;
import com.campusgear.demo.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    void shouldForbidSummaryForStudent() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .with(user("student@campus.edu.pl").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnSummaryForOpiekun() throws Exception {
        when(reportService.getSummary()).thenReturn(new ReportSummaryDTO(
                8L, Map.of("DOSTEPNY", 7L), Map.of(), 2L, 1L, 3L, Map.of(), List.of()));

        mockMvc.perform(get("/api/reports/summary")
                        .with(user("opiekun@campus.edu.pl").roles("OPIEKUN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentTotal").value(8))
                .andExpect(jsonPath("$.loansOverdue").value(1));
    }

    @Test
    void shouldDownloadCsvForOpiekun() throws Exception {
        when(reportService.exportLoansCsv()).thenReturn("id;user\n1;a@campus.edu.pl\n");

        mockMvc.perform(get("/api/reports/loans.csv")
                        .with(user("opiekun@campus.edu.pl").roles("OPIEKUN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("wypozyczenia.csv")))
                .andExpect(content().string("id;user\n1;a@campus.edu.pl\n"));
    }

    @Test
    void shouldForbidCsvForStudent() throws Exception {
        mockMvc.perform(get("/api/reports/loans.csv")
                        .with(user("student@campus.edu.pl").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }
}
