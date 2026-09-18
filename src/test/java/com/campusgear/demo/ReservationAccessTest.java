package com.campusgear.demo;

import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.security.ReservationAccess;
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
class ReservationAccessTest {

    @Mock
    private ReservationEntityRepository reservationRepository;

    @Mock
    private UserEntityRepository userRepository;

    @InjectMocks
    private ReservationAccess reservationAccess;

    private ReservationEntity reservation(Long ownerId) {
        ReservationEntity r = new ReservationEntity();
        r.setId(5L);
        UserEntity owner = new UserEntity();
        owner.setId(ownerId);
        owner.setRole(Role.ROLE_STUDENT);
        r.setUser(owner);
        return r;
    }

    private UserEntity caller(Long id, Role role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    @Test
    void shouldAllowOwner() {
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation(10L)));
        when(userRepository.findByEmail("owner@campus.edu.pl"))
                .thenReturn(Optional.of(caller(10L, Role.ROLE_STUDENT)));

        assertThat(reservationAccess.canCancel(5L, "owner@campus.edu.pl")).isTrue();
    }

    @Test
    void shouldAllowOpiekun() {
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation(10L)));
        when(userRepository.findByEmail("opiekun@campus.edu.pl"))
                .thenReturn(Optional.of(caller(20L, Role.ROLE_OPIEKUN)));

        assertThat(reservationAccess.canCancel(5L, "opiekun@campus.edu.pl")).isTrue();
    }

    @Test
    void shouldDenyForeignStudent() {
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation(10L)));
        when(userRepository.findByEmail("other@campus.edu.pl"))
                .thenReturn(Optional.of(caller(30L, Role.ROLE_STUDENT)));

        assertThatThrownBy(() -> reservationAccess.canCancel(5L, "other@campus.edu.pl"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
