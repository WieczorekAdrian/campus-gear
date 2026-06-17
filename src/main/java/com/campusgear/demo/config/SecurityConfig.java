package com.campusgear.demo.config;

import com.campusgear.demo.config.JwtAuthenticationFilter;
import com.campusgear.demo.config.JwtTokenProvider;
import com.campusgear.demo.repository.UserEntityRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final UserEntityRepository userRepository;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, userRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Standard do bezpiecznego hashowania haseł
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Wyłączamy CSRF (ponieważ używamy bezstanowego JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Wyłączamy sesje - aplikacja nie pamięta stanu po stronie serwera
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. Reguły dostępu (z lotu ptaka)
                .authorizeHttpRequests(auth -> auth
                        // --- DODANE PRZEZ NAS: Odblokowanie Swaggera ---
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // --- ORYGINALNE REGUŁY KACPRA ---
                        .requestMatchers("/api/auth/**").permitAll() // Logowanie i rejestracja otwarta dla każdego
                        .requestMatchers(HttpMethod.GET, "/api/equipment/**").permitAll() // Przeglądanie sprzętu dostępne dla wszystkich
                        .anyRequest().authenticated() // Cała reszta wymaga poprawnego tokenu JWT
                );

        // 4. Wpinamy nasz filtr JWT przed standardowy filtr logowania hasłem
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}