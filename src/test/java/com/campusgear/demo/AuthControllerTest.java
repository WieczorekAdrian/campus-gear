package com.campusgear.demo;

import com.campusgear.demo.controller.AuthController;
import com.campusgear.demo.dto.UserAuthResponseDTO;
import com.campusgear.demo.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ZMIANA TU: Używamy pełnego kontekstu, bo on jest stabilniejszy przy Twoich konfiguracjach
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // To musi zostać, żeby podmienić serwis
    private AuthService authService;

    @Test
    void shouldReturnCurrentUser() throws Exception {
        String email = "test@campus.edu.pl";
        UserAuthResponseDTO mockResponse = new UserAuthResponseDTO(email, "ROLE_STUDENT", "Adam", "Nowak");

        when(authService.getCurrentUser(email)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/auth/me")
                        .with(user(email).roles("STUDENT"))) // Tutaj wstrzykujemy usera
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"));
    }
}