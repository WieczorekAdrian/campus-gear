package com.campusgear.demo.security;

import com.campusgear.demo.entity.LoanEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.exception.ResourceNotFoundException;
import com.campusgear.demo.repository.LoanEntityRepository;
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
public class LoanAccess {

    private final LoanEntityRepository loanRepository;
    private final UserEntityRepository userRepository;

    public boolean canRequestReturn(Long loanId, String email) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono wypożyczenia o ID: " + loanId));

        UserEntity caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika: " + email));

        if (isOwner(loan, caller) || isOpiekun(caller)) {
            return true;
        }
        throw new AccessDeniedException("Brak uprawnień do zgłoszenia zwrotu tego wypożyczenia.");
    }

    private boolean isOwner(LoanEntity loan, UserEntity caller) {
        return loan.getUser().getId().equals(caller.getId());
    }

    private boolean isOpiekun(UserEntity caller) {
        return caller.getRole() == Role.ROLE_OPIEKUN || caller.getRole() == Role.ROLE_ADMIN;
    }
}
