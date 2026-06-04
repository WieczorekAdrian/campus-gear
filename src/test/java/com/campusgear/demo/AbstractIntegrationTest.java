package com.campusgear.demo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    // Definiujemy kontener jako statyczny - dzięki temu jest współdzielony!
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    // Blok statyczny upewnia się, że baza wystartuje tylko raz, przy pierwszym teście
    static {
        postgres.start();
    }
}