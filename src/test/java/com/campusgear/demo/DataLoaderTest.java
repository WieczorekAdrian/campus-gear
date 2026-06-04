package com.campusgear.demo;

import com.campusgear.demo.config.DataLoader;
import com.campusgear.demo.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataLoaderTest extends AbstractIntegrationTest{

    @Autowired
    private UserEntityRepository userRepository;

    @Autowired
    private DataLoader dataLoader; // Wstrzykujemy nasz seeder

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Czyścimy bazę przed każdym testem
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