package com.campusgear.demo.service;

import com.campusgear.demo.entity.LoanEntity;
import com.campusgear.demo.repository.LoanEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final LoanEntityRepository loanRepository;
    private final JavaMailSender mailSender;

    @Value("${notifications.mail.from:wypozyczalnia@campus.edu.pl}")
    private String mailFrom;

    @Value("${notifications.reminder.days-before:2}")
    private int daysBefore;

    /**
     * Codzienne przypomnienia o zbliżającym się terminie zwrotu i po terminie.
     * Błąd wysyłki do jednego użytkownika nie zatrzymuje reszty.
     */
    @Scheduled(cron = "${notifications.reminder.cron:0 0 8 * * *}")
    public void scheduledReminders() {
        int sent = sendReturnReminders();
        log.info("Scheduled reminders finished, sent={}", sent);
    }

    @Transactional(readOnly = true)
    public int sendReturnReminders() {
        LocalDateTime deadline = LocalDateTime.now().plusDays(daysBefore);
        List<LoanEntity> dueLoans =
                loanRepository.findByActualReturnDateIsNullAndExpectedReturnDateBefore(deadline);

        int sent = 0;
        for (LoanEntity loan : dueLoans) {
            if (loan.getUser() == null || loan.getUser().getEmail() == null) {
                continue;
            }
            try {
                sendReminder(loan);
                sent++;
            } catch (Exception ex) {
                log.warn("Failed to send reminder for loan {} to {}", loan.getId(), loan.getUser().getEmail(), ex);
            }
        }

        log.info("Return reminders sent={} out of {} due loans", sent, dueLoans.size());
        return sent;
    }

    private void sendReminder(LoanEntity loan) {
        boolean overdue = loan.getExpectedReturnDate() != null
                && loan.getExpectedReturnDate().isBefore(LocalDateTime.now());

        String subject = overdue
                ? "Campus Gear: termin zwrotu minął"
                : "Campus Gear: zbliża się termin zwrotu";

        String equipment = loan.getEquipment() != null
                ? loan.getEquipment().getDeviceType() + " (" + loan.getEquipment().getSerialNumber() + ")"
                : "sprzęt";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(loan.getUser().getEmail());
        message.setSubject(subject);
        message.setText((overdue
                        ? "Termin zwrotu sprzętu minął. Prosimy o jak najszybszy zwrot.\n"
                        : "Przypominamy o zbliżającym się terminie zwrotu sprzętu.\n")
                + "Sprzęt: " + equipment + "\n"
                + "Termin zwrotu: " + loan.getExpectedReturnDate() + "\n");

        mailSender.send(message);
    }
}
