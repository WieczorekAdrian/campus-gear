package com.campusgear.demo.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:MojBardzoDlugiISekretnyKluczDoPodpisywaniaJWT2026}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}") // 24h
    private long jwtExpirationInMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Generowanie tokenu po zalogowaniu
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();

        // Wyciągamy role i łączymy je w jeden string oddzielony przecinkami (np. "ROLE_STUDENT,ROLE_OPIEKUN")
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles) // <<-- TUTAJ WRZUCAMY ROLE DO TOKENU
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    // Wyciąganie nazwy użytkownika z tokenu
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    // Wyciąganie ról z tokenu
    public String getRolesFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("roles", String.class);
    }

    // Walidacja tokenu (czy nie wygasł, czy podpis się zgadza)
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (Exception ex) {
            // Tutaj w logach możesz obsłużyć ExpiredJwtException, MalformedJwtException itp.
            return false;
        }
    }
}