package com.campusgear.demo.security;

import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.exception.ResourceNotFoundException;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.status.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Reguły dostępu na poziomie instancji (właściciel wiersza lub rola).
 * Wołane z adnotacji {@code @PreAuthorize}, testowane jednostkowo jak zwykły komponent.
 */
@Component
@RequiredArgsConstructor
public class ReservationAccess {

    private final ReservationEntityRepository reservationRepository;
    private final UserEntityRepository userRepository;

    public boolean canCancel(Long reservationId, String email) {
        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono rezerwacji o ID: " + reservationId));

        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        if (isOwner(reservation, caller) || isOpiekun(caller)) {
            return true;
        }
        throw new AccessDeniedException("Brak uprawnień do anulowania tej rezerwacji.");
    }

    private boolean isOwner(ReservationEntity reservation, UserEntity caller) {
        return reservation.getUser().getId().equals(caller.getId());
    }

    private boolean isOpiekun(UserEntity caller) {
        return caller.getRole() == Role.ROLE_OPIEKUN || caller.getRole() == Role.ROLE_ADMIN;
    }
}
