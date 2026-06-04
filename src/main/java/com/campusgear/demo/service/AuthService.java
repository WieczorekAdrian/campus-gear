package com.campusgear.demo.service;

import com.campusgear.demo.dto.LoginDto;
import com.campusgear.demo.dto.RegisterDto;
import com.campusgear.demo.dto.TokenDto;
import com.campusgear.demo.dto.UserAuthResponseDTO;
import com.campusgear.demo.status.Role;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserEntityRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    /**
     * Rejestracja nowego użytkownika (domyślnie z rolą ROLE_STUDENT)
     */
    public void register(RegisterDto dto) {
        // 1. Sprawdzamy, czy email jest już zajęty
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        // 2. Tworzymy nową encję użytkownika
        UserEntity user = new UserEntity();
        user.setEmail(dto.email());
        // Szyfrujemy hasło przed zapisem do bazy przy użyciu BCrypt
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setRole(Role.ROLE_STUDENT); // Każdy rejestrujący się dostaje domyślnie rolę studenta

        userRepository.save(user);
    }

    /**
     * Logowanie tradycyjne - weryfikacja danych i generowanie tokenu JWT
     */
    public TokenDto login(LoginDto dto) {
        // 1. Szukamy użytkownika w bazie po adresie email
        UserEntity user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        // 2. Sprawdzamy, czy podane hasło pasuje do hashu zapisanego w bazie
        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        // 3. Tworzymy obiekt Authentication, którego Twój JwtTokenProvider potrzebuje do wyciągnięcia ról
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                user.getAuthorities()
        );

        // 4. Generujemy token JWT i pakujemy go w TokenDto
        String token = tokenProvider.generateToken(authentication);
        return new TokenDto(token);
    }
    public UserAuthResponseDTO getCurrentUser(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        // Tu dzieje się całe mapowanie. Kontroler o tym nie wie.
        return new UserAuthResponseDTO(
                user.getEmail(),
                user.getRole().toString(),
                user.getFirstName(),
                user.getLastName()
        );
    }
}