package com.campusgear.demo.service;

import com.campusgear.demo.dto.EquipmentRequestDTO;
import com.campusgear.demo.dto.EquipmentResponseDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.mapper.EquipmentMapper;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentEntityRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper; // Wstrzykujemy naszego Mappera!

    @Transactional
    public EquipmentResponseDTO addEquipment(EquipmentRequestDTO requestDTO) {
        // 1. MapStruct automatycznie tłumaczy DTO na Encję
        EquipmentEntity entity = equipmentMapper.toEntity(requestDTO);

        // 2. Zapisujemy do bazy
        EquipmentEntity savedEntity = equipmentRepository.save(entity);

        // 3. MapStruct tłumaczy zapisany sprzęt na odpowiedź dla frontendu
        return equipmentMapper.toResponseDTO(savedEntity);
    }

    @Transactional
    public EquipmentResponseDTO updateEquipment(Long id, EquipmentRequestDTO requestDTO) {
        // 1. Znajdujemy stary sprzęt w bazie
        EquipmentEntity entity = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono sprzętu o ID: " + id));

        // 2. MapStruct magicznie nadpisuje pola w znalezionej encji nowymi danymi z DTO!
        equipmentMapper.updateEntityFromDto(requestDTO, entity);

        // 3. Zapis i zwrot
        EquipmentEntity updatedEntity = equipmentRepository.save(entity);
        return equipmentMapper.toResponseDTO(updatedEntity);
    }
}