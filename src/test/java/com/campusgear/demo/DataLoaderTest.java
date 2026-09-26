package com.campusgear.demo;

import com.campusgear.demo.config.DataLoader;
import com.campusgear.demo.repository.DefectReportEntityRepository;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.repository.LoanEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataLoaderTest extends AbstractIntegrationTest{

    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private LoanEntityRepository loanRepository;

    @Autowired
    private ReservationEntityRepository reservationRepository;

    @Autowired
    private DefectReportEntityRepository defectReportRepository;

    @Autowired
    private EquipmentEntityRepository equipmentRepository;

    @Autowired
    private DataLoader dataLoader; // Wstrzykujemy nasz seeder

    @BeforeEach
    void setUp() {
        // Czyścimy całą bazę w kolejności FK (kontener Postgres jest współdzielony).
        loanRepository.deleteAll();
        reservationRepository.deleteAll();
        defectReportRepository.deleteAll();
        equipmentRepository.deleteAll();
        userRepository.deleteAll();
        dataLoader.run();           // Ręcznie uruchamiamy seeder, żeby mieć pewność!
    }

    @Test
    void shouldHaveSeeded10Users() {
        long count = userRepository.count();
        assertThat(count).isEqualTo(10);
    }

    @Test
    void shouldHaveCorrectUserCreated() {
        var user = userRepository.findByEmail("user1@campus.edu.pl");
        assertThat(user).isPresent();
    }
}