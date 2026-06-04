package com.campusgear.demo;

import com.campusgear.demo.dto.ReservationRequestDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.service.ReservationService;
import com.campusgear.demo.status.EquipmentStatus;
import com.campusgear.demo.status.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private EquipmentEntityRepository equipmentRepository;

    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private ReservationEntityRepository reservationRepository;

    private UserEntity testUser;
    private EquipmentEntity testEquipment;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        equipmentRepository.deleteAll();
        userRepository.deleteAll();

        // Stworzenie użytkownika testowego
        testUser = new UserEntity();
        testUser.setEmail("concurrency.test@campus.edu.pl");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.ROLE_STUDENT);
        testUser = userRepository.save(testUser);

        // Stworzenie sprzętu testowego
        testEquipment = new EquipmentEntity();
        testEquipment.setDeviceType("Laptop");
        testEquipment.setTechnicalSpecification("Dell XPS");
        testEquipment.setSerialNumber("DELL-12345");
        testEquipment.setLocation("Main Hall");
        testEquipment.setStatus(EquipmentStatus.DOSTEPNY);
        testEquipment.setMaxRentalDays(10);
        testEquipment = equipmentRepository.save(testEquipment);
    }

    @Test
    void shouldPreventDoubleBookingUnderConcurrentRequests() throws InterruptedException {
        // Given
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(3);
        ReservationRequestDTO dto = new ReservationRequestDTO(testEquipment.getId(), start, end);

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errorMessages = new CopyOnWriteArrayList<>();

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Czekaj na sygnał startu
                    reservationService.createReservation(dto, testUser);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    errorMessages.add(e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start wątków jednocześnie
        finishLatch.await(5, TimeUnit.SECONDS); // Czekaj na zakończenie wszystkich wątków
        executorService.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(errorMessages).contains("Ten sprzęt jest już zarezerwowany w tym terminie.");
    }
}
