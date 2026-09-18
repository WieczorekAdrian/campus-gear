package com.campusgear.demo;

import com.campusgear.demo.entity.LoanEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.LoanEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.security.LoanAccess;
import com.campusgear.demo.status.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanAccessTest {

    @Mock
    private LoanEntityRepository loanRepository;

    @Mock
    private UserEntityRepository userRepository;

    @InjectMocks
    private LoanAccess loanAccess;

    private LoanEntity loan(Long ownerId) {
        LoanEntity loan = new LoanEntity();
        loan.setId(50L);
        UserEntity owner = new UserEntity();
        owner.setId(ownerId);
        owner.setRole(Role.ROLE_STUDENT);
        loan.setUser(owner);
        return loan;
    }

    private UserEntity caller(Long id, Role role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    @Test
    void shouldAllowOwner() {
        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan(10L)));
        when(userRepository.findByEmail("owner@campus.edu.pl"))
                .thenReturn(Optional.of(caller(10L, Role.ROLE_STUDENT)));

        assertThat(loanAccess.canRequestReturn(50L, "owner@campus.edu.pl")).isTrue();
    }

    @Test
    void shouldAllowOpiekun() {
        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan(10L)));
        when(userRepository.findByEmail("opiekun@campus.edu.pl"))
                .thenReturn(Optional.of(caller(20L, Role.ROLE_OPIEKUN)));

        assertThat(loanAccess.canRequestReturn(50L, "opiekun@campus.edu.pl")).isTrue();
    }

    @Test
    void shouldDenyForeignStudent() {
        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan(10L)));
        when(userRepository.findByEmail("other@campus.edu.pl"))
                .thenReturn(Optional.of(caller(30L, Role.ROLE_STUDENT)));

        assertThatThrownBy(() -> loanAccess.canRequestReturn(50L, "other@campus.edu.pl"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
