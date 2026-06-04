package com.campusgear.demo.config;

import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.UserEntityRepository;

import com.campusgear.demo.status.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserEntityRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Sprawdzamy, czy baza jest pusta – żeby nie dodawać userów przy każdym restarcie!
        if (userRepository.count() == 0) {

            IntStream.rangeClosed(1, 10).forEach(i -> {
                UserEntity user = new UserEntity();
                user.setEmail("user" + i + "@campus.edu.pl");
                // Używamy passwordEncoder, bo UserDetails oczekuje zahashowanego hasła
                user.setPasswordHash(passwordEncoder.encode("Password123!"));
                user.setFirstName("Imie" + i);
                user.setLastName("Nazwisko" + i);

                // Przypisanie roli (np. co drugi jest opiekunem, reszta studentami)
                user.setRole(i % 2 == 0 ? Role.ROLE_OPIEKUN : Role.ROLE_STUDENT);

                userRepository.save(user);
            });

            System.out.println(">>> Seedowanie bazy zakończone: Dodano 10 użytkowników.");
        }
    }
}