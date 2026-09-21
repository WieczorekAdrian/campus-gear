package com.campusgear.demo;

import com.campusgear.demo.dto.EquipmentResponseDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.mapper.EquipmentMapper;
import com.campusgear.demo.repository.EquipmentEntityRepository;
import com.campusgear.demo.service.EquipmentService;
import com.campusgear.demo.status.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentEntityRepository equipmentRepository;

    @Mock
    private EquipmentMapper equipmentMapper;

    @InjectMocks
    private EquipmentService equipmentService;

    @Test
    void shouldSearchWithAllCriteriaAndMapResults() {
        EquipmentEntity entity = new EquipmentEntity();
        entity.setLocation("Magazyn Główny - Regał 1");
        EquipmentResponseDTO dto = new EquipmentResponseDTO();
        when(equipmentRepository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(equipmentMapper.toResponseDTO(entity)).thenReturn(dto);

        List<EquipmentResponseDTO> result =
                equipmentService.searchEquipment(EquipmentStatus.DOSTEPNY, "Laptop", "magazyn");

        assertThat(result).containsExactly(dto);
    }

    @Test
    void shouldPassFiltersIntoSpecification() {
        when(equipmentRepository.findAll(any(Specification.class))).thenReturn(List.of());

        equipmentService.searchEquipment(EquipmentStatus.DOSTEPNY, "Laptop", "Studio");

        ArgumentCaptor<Specification<EquipmentEntity>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(equipmentRepository).findAll(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    void shouldBuildEmptySpecificationWhenNoFilters() {
        when(equipmentRepository.findAll(any(Specification.class))).thenReturn(List.of());

        List<EquipmentResponseDTO> result = equipmentService.searchEquipment(null, null, "   ");

        assertThat(result).isEmpty();
        ArgumentCaptor<Specification<EquipmentEntity>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(equipmentRepository).findAll(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }
}
