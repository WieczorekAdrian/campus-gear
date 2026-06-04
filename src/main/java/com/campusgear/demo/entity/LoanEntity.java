package com.campusgear.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime borrowDate;
    private LocalDateTime expectedReturnDate;
    private LocalDateTime actualReturnDate; // Nullable, dopóki sprzęt nie wróci

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "equipment_id", nullable = false)
    private EquipmentEntity equipment;

    @OneToOne
    @JoinColumn(name = "reservation_id") // Opcjonalne, jeśli wypożyczono z rezerwacji
    private ReservationEntity reservation;
}