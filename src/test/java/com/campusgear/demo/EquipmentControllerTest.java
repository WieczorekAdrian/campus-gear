package com.campusgear.demo;

import com.campusgear.demo.service.EquipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EquipmentControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipmentService equipmentService;

    @Test
    void shouldPassLocationFilterToService() throws Exception {
        when(equipmentService.searchEquipment(isNull(), isNull(), eq("Magazyn"))).thenReturn(List.of());

        mockMvc.perform(get("/api/equipment/search").param("location", "Magazyn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(equipmentService).searchEquipment(isNull(), isNull(), eq("Magazyn"));
    }

    @Test
    void shouldCombineStatusAndLocation() throws Exception {
        when(equipmentService.searchEquipment(
                        eq(com.campusgear.demo.status.EquipmentStatus.DOSTEPNY), isNull(), eq("Studio")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/equipment/search")
                        .param("status", "DOSTEPNY")
                        .param("location", "Studio"))
                .andExpect(status().isOk());

        verify(equipmentService).searchEquipment(
                eq(com.campusgear.demo.status.EquipmentStatus.DOSTEPNY), isNull(), eq("Studio"));
    }
}
