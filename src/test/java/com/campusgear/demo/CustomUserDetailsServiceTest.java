package com.campusgear.demo;

import com.campusgear.demo.config.CustomUserDetailsService;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.status.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserEntityRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldLoadUserByEmail() {
        UserEntity user = new UserEntity();
        user.setEmail("a@campus.edu.pl");
        user.setRole(Role.ROLE_STUDENT);
        when(userRepository.findByEmail("a@campus.edu.pl")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("a@campus.edu.pl");

        assertThat(result.getUsername()).isEqualTo("a@campus.edu.pl");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("nope@campus.edu.pl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nope@campus.edu.pl"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
