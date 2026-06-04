package com.campusgear.demo.controller;

import com.campusgear.demo.dto.EquipmentRequestDTO;
import com.campusgear.demo.dto.EquipmentResponseDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.service.EquipmentService;
import com.campusgear.demo.status.EquipmentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentEntityRepository equipmentEntityRepository;
    private final EquipmentService equipmentService; // Dodajemy nasz serwis!

    public EquipmentController(EquipmentEntityRepository equipmentEntityRepository, EquipmentService equipmentService) {
        this.equipmentEntityRepository = equipmentEntityRepository;
        this.equipmentService = equipmentService;
    }

    // --- METODY DO ODCZYTU (Dostępne dla wszystkich) ---

    @GetMapping
    public List<EquipmentEntity> getAllEquipment() {
        return equipmentEntityRepository.findAll();
    }

    @GetMapping("/search")
    public List<EquipmentEntity> searchEquipment(
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) String deviceType) {

        if (status != null && deviceType != null) {
            return equipmentEntityRepository.findByStatusAndDeviceType(status, deviceType);
        } else if (status != null) {
            return equipmentEntityRepository.findByStatus(status);
        } else if (deviceType != null) {
            return equipmentEntityRepository.findByDeviceType(deviceType);
        }

        return equipmentEntityRepository.findAll();
    }

    // --- METODY MODYFIKUJĄCE (Tylko dla Opiekuna) ---

    @PostMapping
    @PreAuthorize("hasRole('OPIEKUN')") // Zabezpieczenie: tylko ta rola ma dostęp
    public ResponseEntity<EquipmentResponseDTO> addEquipment(@RequestBody EquipmentRequestDTO equipment) {
        EquipmentResponseDTO response = equipmentService.addEquipment(equipment);
        // Zwracamy kod 201 (CREATED) zamiast domyślnego 200 (OK) - bardzo profesjonalna praktyka!
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPIEKUN')") // Zabezpieczenie: tylko ta rola ma dostęp
    public ResponseEntity<EquipmentResponseDTO> updateEquipment(@PathVariable Long id, @RequestBody EquipmentRequestDTO updatedEquipment) {
        EquipmentResponseDTO response = equipmentService.updateEquipment(id, updatedEquipment);
        return ResponseEntity.ok(response); // Zwracamy kod 200 (OK)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPIEKUN')") // Warto od razu zabezpieczyć też usuwanie!
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentEntityRepository.deleteById(id);
        return ResponseEntity.noContent().build(); // Zwracamy kod 204 (NO CONTENT) - standard przy usuwaniu
    }
}