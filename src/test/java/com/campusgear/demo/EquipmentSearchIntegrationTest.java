package com.campusgear.demo;

import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.specification.EquipmentSpecs;
import com.campusgear.demo.status.EquipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regresja: @Query z NULL w location bindowało bytea i Postgres rzucał
 * "function lower(bytea) does not exist" (na froncie mylący 403 przez /error).
 * Te testy jadą na prawdziwym Postgresie (Testcontainers), nie na mockach.
 */
@SpringBootTest
class EquipmentSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EquipmentEntityRepository equipmentRepository;

    @BeforeEach
    void setUp() {
        equipmentRepository.deleteAll();

        equipmentRepository.save(item("Laptop", "Magazyn Główny", EquipmentStatus.DOSTEPNY));
        equipmentRepository.save(item("Projektor", "Studio Nagrań A", EquipmentStatus.DOSTEPNY));
        equipmentRepository.save(item("Kamera", "Studio Nagrań A", EquipmentStatus.SERWISOWANY));
    }

    private EquipmentEntity item(String type, String location, EquipmentStatus status) {
        EquipmentEntity e = new EquipmentEntity();
        e.setDeviceType(type);
        e.setTechnicalSpecification("spec");
        e.setSerialNumber("SN-" + type + "-" + location.hashCode());
        e.setLocation(location);
        e.setStatus(status);
        return e;
    }

    @Test
    void shouldReturnAllWhenAllCriteriaNull() {
        List<EquipmentEntity> result =
                equipmentRepository.findAll(EquipmentSpecs.withFilters(null, null, null));

        assertThat(result).hasSize(3);
    }

    @Test
    void shouldFilterByLocationPartiallyAndCaseInsensitively() {
        List<EquipmentEntity> result =
                equipmentRepository.findAll(EquipmentSpecs.withFilters(null, null, "studio nagrań"));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> e.getLocation().equals("Studio Nagrań A"));
    }

    @Test
    void shouldCombineStatusAndLocation() {
        List<EquipmentEntity> result = equipmentRepository
                .findAll(EquipmentSpecs.withFilters(EquipmentStatus.DOSTEPNY, null, "Studio"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceType()).isEqualTo("Projektor");
    }

    @Test
    void shouldFilterByStatusAlone() {
        List<EquipmentEntity> result = equipmentRepository
                .findAll(EquipmentSpecs.withFilters(EquipmentStatus.SERWISOWANY, null, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceType()).isEqualTo("Kamera");
    }
}
