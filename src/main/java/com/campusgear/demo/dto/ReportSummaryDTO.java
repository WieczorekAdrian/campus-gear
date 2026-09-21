package com.campusgear.demo.dto;

import java.util.List;
import java.util.Map;

public record ReportSummaryDTO(
        long equipmentTotal,
        Map<String, Long> equipmentByStatus,
        Map<String, Long> reservationsByStatus,
        long loansActive,
        long loansOverdue,
        long loansReturned,
        Map<String, Long> defectsByStatus,
        List<TopEquipmentDTO> topEquipment
) {
    public record TopEquipmentDTO(
            Long id,
            String deviceType,
            String serialNumber,
            long loans
    ) {
    }
}
