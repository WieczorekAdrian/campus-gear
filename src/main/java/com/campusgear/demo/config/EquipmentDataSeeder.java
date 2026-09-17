package com.campusgear.demo.config;

import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.status.EquipmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
public class EquipmentDataSeeder implements CommandLineRunner {

    private final EquipmentEntityRepository equipmentRepository;

    @Override
    public void run(String... args) {
        // Sprzęt nie jest do nikogo przypisany - leży w wypożyczalni.
        // ładujemy tylko przy pustej tabeli, żeby nie dublować przy restarcie.
        if (equipmentRepository.count() > 0) {
            return;
        }

        List<EquipmentEntity> seed = List.of(
                item("Laptop", "Dell Latitude 5540, i7/16GB/512GB", "DLL-5540-001", "Magazyn Główny - Regał 1", EquipmentStatus.DOSTEPNY, 14),
                item("Laptop", "Lenovo ThinkPad E16, Ryzen 7/16GB/512GB", "LNV-E16-002", "Magazyn Główny - Regał 1", EquipmentStatus.DOSTEPNY, 14),
                item("Projektor", "Epson EB-X49, 3600lm, HDMI/VGA", "EPS-X49-003", "Magazyn Główny - Regał 2", EquipmentStatus.DOSTEPNY, 7),
                item("Aparat", "Sony A7 III + obiektyw 28-70mm", "SNY-A73-004", "Magazyn Główny - Regał 2", EquipmentStatus.DOSTEPNY, 7),
                item("Interfejs Audio", "Focusrite Scarlett Solo 3rd Gen", "FSS-3G-005", "Studio Nagrań A", EquipmentStatus.DOSTEPNY, 7),
                item("Mikrofon", "Shure SM58 + statyw i kabel XLR", "SHR-SM58-006", "Studio Nagrań A", EquipmentStatus.SERWISOWANY, 7),
                item("Kamera", "Canon XA40, 4K, torba + 2x bateria", "CNN-XA40-007", "Magazyn Główny - Regał 3", EquipmentStatus.DOSTEPNY, 7),
                item("Statyw", "Manfrotto MT055XPRO3 + głowica", "MNF-055-008", "Magazyn Główny - Regał 3", EquipmentStatus.DOSTEPNY, 14)
        );

        equipmentRepository.saveAll(seed);
        System.out.println(">>> Seedowanie bazy zakończone: Dodano " + seed.size() + " sprzętów.");
    }

    private EquipmentEntity item(String deviceType, String spec, String serial,
                                 String location, EquipmentStatus status, int maxRentalDays) {
        EquipmentEntity e = new EquipmentEntity();
        e.setDeviceType(deviceType);
        e.setTechnicalSpecification(spec);
        e.setSerialNumber(serial);
        e.setLocation(location);
        e.setStatus(status);
        e.setMaxRentalDays(maxRentalDays);
        e.setAcademicAccount(false);
        return e;
    }
}
