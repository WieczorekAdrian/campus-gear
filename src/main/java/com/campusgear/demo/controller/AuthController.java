package com.campusgear.demo.controller;

import com.campusgear.demo.dto.LoginDto;
import com.campusgear.demo.dto.RegisterDto;
import com.campusgear.demo.dto.TokenDto;
import com.campusgear.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint dla rejestracji tradycyjnej.
     * Zwraca HTTP 21 Created po pomyślnym zapisie użytkownika.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterDto dto) {
        authService.register(dto);
        return new ResponseEntity<>("Registration successful!", HttpStatus.CREATED);
    }

    /**
     * Endpoint dla logowania tradycyjnego.
     * Zwraca HTTP 200 OK oraz wygenerowany token JWT w ciele odpowiedzi.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenDto> loginUser(@Valid @RequestBody LoginDto dto) {
        TokenDto tokenDto = authService.login(dto);
        return ResponseEntity.ok(tokenDto);
    }
}