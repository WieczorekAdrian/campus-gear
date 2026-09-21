package com.campusgear.demo;

import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.LoanEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.LoanEntityRepository;
import com.campusgear.demo.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private LoanEntityRepository loanRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    private LoanEntity loan(String email) {
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setDeviceType("Laptop");
        equipment.setSerialNumber("DLL-001");
        UserEntity user = email == null ? null : new UserEntity();
        if (user != null) {
            user.setEmail(email);
        }
        LoanEntity loan = new LoanEntity();
        loan.setId(1L);
        loan.setEquipment(equipment);
        loan.setUser(user);
        loan.setExpectedReturnDate(LocalDateTime.now().minusDays(1));
        return loan;
    }

    @Test
    void shouldSendReminderForOverdueLoan() {
        when(loanRepository.findByActualReturnDateIsNullAndExpectedReturnDateBefore(any()))
                .thenReturn(List.of(loan("student@campus.edu.pl")));

        int sent = notificationService.sendReturnReminders();

        assertThat(sent).isEqualTo(1);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("student@campus.edu.pl");
        assertThat(captor.getValue().getSubject()).contains("minął");
    }

    @Test
    void shouldSkipLoansWithoutRecipient() {
        when(loanRepository.findByActualReturnDateIsNullAndExpectedReturnDateBefore(any()))
                .thenReturn(List.of(loan(null)));

        int sent = notificationService.sendReturnReminders();

        assertThat(sent).isEqualTo(0);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldContinueAfterSingleFailure() {
        when(loanRepository.findByActualReturnDateIsNullAndExpectedReturnDateBefore(any()))
                .thenReturn(List.of(loan("a@campus.edu.pl"), loan("b@campus.edu.pl")));
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        int sent = notificationService.sendReturnReminders();

        assertThat(sent).isEqualTo(0);
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
}
