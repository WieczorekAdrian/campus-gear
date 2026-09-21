package com.campusgear.demo.controller;

import com.campusgear.demo.dto.RemindersSentDTO;
import com.campusgear.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/reminders")
    @PreAuthorize("hasAnyRole('OPIEKUN', 'ADMIN')")
    public ResponseEntity<RemindersSentDTO> sendReminders() {
        return ResponseEntity.ok(new RemindersSentDTO(notificationService.sendReturnReminders()));
    }
}
