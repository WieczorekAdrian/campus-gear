package com.campusgear.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

public class TestSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // Wyłączamy CSRF do testów
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // Pozwalamy na wszystko
                .build();
    }
}