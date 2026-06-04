package com.campusgear.demo.controller;

import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.status.EquipmentStatus;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentEntityRepository equipmentEntityRepository;

    // Wstrzykiwanie zależności z nową nazwą repozytorium
    public EquipmentController(EquipmentEntityRepository equipmentEntityRepository) {
        this.equipmentEntityRepository = equipmentEntityRepository;
    }

    // Endpoint do POBIERANIA wszystkich sprzętów
    @GetMapping
    public List<EquipmentEntity> getAllEquipment() {
        return equipmentEntityRepository.findAll();
    }

    // Endpoint do DODAWANIA nowego sprzętu
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentEntity addEquipment(@RequestBody EquipmentEntity equipment) {
        return equipmentEntityRepository.save(equipment);
    }

    // Endpoint do EDYCJI sprzętu (Żądanie PUT)
    @PutMapping("/{id}")
    public EquipmentEntity updateEquipment(@PathVariable Long id, @RequestBody EquipmentEntity updatedEquipment) {
        return equipmentEntityRepository.findById(id)
                .map(equipment -> {
                    // Aktualizujemy dane znalezionego sprzętu nowymi wartościami
                    equipment.setDeviceType(updatedEquipment.getDeviceType());
                    equipment.setTechnicalSpecification(updatedEquipment.getTechnicalSpecification());
                    equipment.setSerialNumber(updatedEquipment.getSerialNumber());
                    equipment.setLocation(updatedEquipment.getLocation());
                    equipment.setStatus(updatedEquipment.getStatus());
                    // Zapisujemy zaktualizowany obiekt do bazy
                    return equipmentEntityRepository.save(equipment);
                })
                .orElseThrow(() -> new RuntimeException("Nie znaleziono sprzętu o ID: " + id));
    }

    // Endpoint do USUWANIA sprzętu (Żądanie DELETE)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEquipment(@PathVariable Long id) {
        equipmentEntityRepository.deleteById(id);
    }

    // Endpoint do WYSZUKIWANIA sprzętu po statusie (Żądanie GET)
    // Przykład: http://localhost:8080/api/equipment/search/status?value=DOSTĘPNY
    @GetMapping("/search/status")
    public List<EquipmentEntity> getEquipmentByStatus(@RequestParam EquipmentStatus value) {
        return equipmentEntityRepository.findByStatus(value);
    }

    // Endpoint do WYSZUKIWANIA sprzętu po typie (Żądanie GET)
    // Przykład: http://localhost:8080/api/equipment/search/type?value=Laptop
    @GetMapping("/search/type")
    public List<EquipmentEntity> getEquipmentByType(@RequestParam String value) {
        return equipmentEntityRepository.findByDeviceType(value);
    }

}