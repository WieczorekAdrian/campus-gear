package com.campusgear.demo;

import com.campusgear.demo.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void shouldForbidRemindersForStudent() throws Exception {
        mockMvc.perform(post("/api/notifications/reminders")
                        .with(user("student@campus.edu.pl").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldSendRemindersForOpiekun() throws Exception {
        when(notificationService.sendReturnReminders()).thenReturn(3);

        mockMvc.perform(post("/api/notifications/reminders")
                        .with(user("opiekun@campus.edu.pl").roles("OPIEKUN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(3));
    }
}
