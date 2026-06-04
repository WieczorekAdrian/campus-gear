package com.campusgear.demo;

import com.campusgear.demo.dto.LoginDto;
import com.campusgear.demo.dto.RegisterDto;
import com.campusgear.demo.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.notNullValue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Usuwamy @SpringBootTest, bo dziedziczymy go z klasy abstrakcyjnej!
@AutoConfigureMockMvc
@SpringBootTest
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserEntityRepository userRepository;


    @BeforeEach
    void setUp() {
        // Czyszczenie bazy przed każdym testem w danej klasie
        userRepository.deleteAll();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        // Given
        RegisterDto registerDto = new RegisterDto(
                "testuser@campus.edu.pl",
                "securePass123",
                "John",
                "Doe"
        );

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Registration successful!"));
    }

    @Test
    void shouldNotRegisterUserWhenEmailAlreadyExists() throws Exception {
        // Given
        RegisterDto firstRegistration = new RegisterDto(
                "duplicate@campus.edu.pl",
                "password123",
                "Adam",
                "Nowak"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRegistration)))
                .andExpect(status().isCreated());

        RegisterDto duplicateDto = new RegisterDto(
                "duplicate@campus.edu.pl",
                "newPassword789",
                "Ewa",
                "Kowalska"
        );

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginSuccessfullyAndReturnToken() throws Exception {
        // Given
        RegisterDto registerDto = new RegisterDto(
                "login.test@campus.edu.pl",
                "mySecretPassword",
                "Anna",
                "Zielińska"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated());

        LoginDto loginDto = new LoginDto("login.test@campus.edu.pl", "mySecretPassword");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void shouldNotLoginWithInvalidCredentials() throws Exception {
        // Given
        LoginDto invalidLoginDto = new LoginDto("wrong.user@campus.edu.pl", "badPassword");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLoginDto)))
                .andExpect(status().isBadRequest());
    }
}