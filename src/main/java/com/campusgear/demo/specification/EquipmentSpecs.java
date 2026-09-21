package com.campusgear.demo.specification;

import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.status.EquipmentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamiczne filtry sprzętu. Predykat doklejany tylko dla podanego kryterium,
 * więc nie ma problemu z NULL-ami (w przeciwieństwie do jednego @Query ze stringa,
 * gdzie NULL bindował się jako bytea i Postgres rzucał "function lower(bytea) does not exist").
 * Parametry idą z Javy z właściwym typem (String), puste teksty traktujemy jak brak filtra.
 */
public final class EquipmentSpecs {

    private EquipmentSpecs() {
    }

    public static Specification<EquipmentEntity> withFilters(
            EquipmentStatus status, String deviceType, String location) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (deviceType != null && !deviceType.isBlank()) {
                predicates.add(cb.equal(root.get("deviceType"), deviceType.trim()));
            }

            String normalizedLocation = location == null ? null : location.trim();
            if (normalizedLocation != null && !normalizedLocation.isEmpty()) {
                predicates.add(cb.like(
                        cb.lower(root.get("location")),
                        "%" + normalizedLocation.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
