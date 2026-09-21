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
    // Zwracamy DTO (bez listy rezerwacji), żeby nie zapętlać JSON encja<->rezerwacje.

    @GetMapping
    public List<EquipmentResponseDTO> getAllEquipment() {
        return equipmentService.getAllEquipment();
    }

    @GetMapping("/search")
    public List<EquipmentResponseDTO> searchEquipment(
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String location) {

        return equipmentService.searchEquipment(status, deviceType, location);
    }

    // --- METODY MODYFIKUJĄCE (Tylko dla Opiekuna) ---

    @PostMapping
    @PreAuthorize("hasRole('OPIEKUN')")
    public ResponseEntity<EquipmentResponseDTO> addEquipment(@RequestBody EquipmentRequestDTO equipment) {
        EquipmentResponseDTO response = equipmentService.addEquipment(equipment);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPIEKUN')") // Zabezpieczenie: tylko ta rola ma dostęp
    public ResponseEntity<EquipmentResponseDTO> updateEquipment(@PathVariable Long id, @RequestBody EquipmentRequestDTO updatedEquipment) {
        EquipmentResponseDTO response = equipmentService.updateEquipment(id, updatedEquipment);
        return ResponseEntity.ok(response); // Zwracamy kod 200 (OK)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPIEKUN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEquipment(@PathVariable Long id) {
        equipmentEntityRepository.deleteById(id);
    }
}