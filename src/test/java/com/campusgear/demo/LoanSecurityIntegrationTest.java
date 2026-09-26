package com.campusgear.demo;

import com.campusgear.demo.dto.LoginDto;
import com.campusgear.demo.dto.RegisterDto;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.LoanEntity;
import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.DefectReportEntityRepository;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.LoanEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.status.EquipmentStatus;
import com.campusgear.demo.status.ReservationStatus;
import com.campusgear.demo.status.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dowód, że reguły ról z adnotacji {@code @PreAuthorize} działają na żywym
 * łańcuchu filtr JWT -> proxy serwisu (testy jednostkowe ich nie widzą).
 */
@AutoConfigureMockMvc
@SpringBootTest
class LoanSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private EquipmentEntityRepository equipmentRepository;

    @Autowired
    private ReservationEntityRepository reservationRepository;

    @Autowired
    private LoanEntityRepository loanRepository;

    @Autowired
    private DefectReportEntityRepository defectReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EquipmentEntity testEquipment;
    private ReservationEntity testReservation;

    @BeforeEach
    void setUp() throws Exception {
        loanRepository.deleteAll();
        reservationRepository.deleteAll();
        defectReportRepository.deleteAll();
        equipmentRepository.deleteAll();
        userRepository.deleteAll();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterDto(
                                "student@campus.edu.pl", "Password123!", "Jan", "Kowalski"))))
                .andExpect(status().isCreated());

        UserEntity opiekun = new UserEntity();
        opiekun.setEmail("opiekun@campus.edu.pl");
        opiekun.setPasswordHash(passwordEncoder.encode("Password123!"));
        opiekun.setFirstName("Anna");
        opiekun.setLastName("Nowak");
        opiekun.setRole(Role.ROLE_OPIEKUN);
        userRepository.save(opiekun);

        testEquipment = new EquipmentEntity();
        testEquipment.setDeviceType("Laptop");
        testEquipment.setSerialNumber("SEC-001");
        testEquipment.setStatus(EquipmentStatus.DOSTEPNY);
        testEquipment = equipmentRepository.save(testEquipment);

        UserEntity student = userRepository.findByEmail("student@campus.edu.pl").orElseThrow();
        testReservation = new ReservationEntity();
        testReservation.setUser(student);
        testReservation.setEquipment(testEquipment);
        testReservation.setStartDate(LocalDateTime.now().plusDays(1));
        testReservation.setEndDate(LocalDateTime.now().plusDays(3));
        testReservation.setStatus(ReservationStatus.AKTYWNA);
        testReservation = reservationRepository.save(testReservation);
    }

    private String token(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginDto(email, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    @Test
    void studentCannotIssueLoan() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .header("Authorization", "Bearer " + token("student@campus.edu.pl"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":" + testReservation.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void opiekunIssuesAndConfirmsWhileStudentCannotConfirm() throws Exception {
        String opiekunToken = token("opiekun@campus.edu.pl");
        String studentToken = token("student@campus.edu.pl");

        MvcResult issued = mockMvc.perform(post("/api/loans")
                        .header("Authorization", "Bearer " + opiekunToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":" + testReservation.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipment.deviceType").value("Laptop"))
                .andReturn();
        long loanId = objectMapper.readTree(issued.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/loans/" + loanId + "/confirm-return")
                        .header("Authorization", "Bearer " + studentToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"damaged\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/loans/" + loanId + "/confirm-return")
                        .header("Authorization", "Bearer " + opiekunToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"damaged\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualReturnDate").exists());
    }
}
