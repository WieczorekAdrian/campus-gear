package com.campusgear.demo.repository;

import com.campusgear.demo.entity.LoanEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoanEntityRepository extends JpaRepository<LoanEntity, Long> {

    boolean existsByReservationId(Long reservationId);

    long countByActualReturnDateIsNull();

    long countByActualReturnDateIsNullAndExpectedReturnDateBefore(LocalDateTime now);

    @EntityGraph(attributePaths = "equipment")
    List<LoanEntity> findByUser_EmailOrderByBorrowDateDesc(String email);

    @EntityGraph(attributePaths = {"equipment", "user"})
    List<LoanEntity> findByActualReturnDateIsNullOrderByBorrowDateDesc();

    @EntityGraph(attributePaths = {"equipment", "user"})
    List<LoanEntity> findAllByOrderByBorrowDateDesc();

    @EntityGraph(attributePaths = {"equipment", "user"})
    List<LoanEntity> findByActualReturnDateIsNullAndExpectedReturnDateBefore(
            LocalDateTime deadline);
}