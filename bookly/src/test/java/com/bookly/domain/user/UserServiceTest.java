package com.bookly.domain.user;

import com.bookly.exception.ConflictException;
import com.bookly.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @Test
    void assertEmailAvailable_emailExists_throwsConflict() {
        when(userRepository.existsByEmail("taken@email.pl")).thenReturn(true);

        assertThatThrownBy(() -> userService.assertEmailAvailable("taken@email.pl"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("email already registered");
    }

    @Test
    void assertEmailAvailable_emailFree_doesNotThrow() {
        when(userRepository.existsByEmail("free@email.pl")).thenReturn(false);

        userService.assertEmailAvailable("free@email.pl");
    }

    @Test
    void createOwner_savesUserWithCorrectFields() {
        UUID tenantId = UUID.randomUUID();
        User saved = new User();
        saved.setId(UUID.randomUUID());
        saved.setTenantId(tenantId);
        saved.setEmail("owner@test.pl");
        saved.setRole(UserRole.OWNER);
        when(userRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(saved);

        User result = userService.createOwner(tenantId, "owner@test.pl", "hashed");

        assertThat(result.getEmail()).isEqualTo("owner@test.pl");
        assertThat(result.getRole()).isEqualTo(UserRole.OWNER);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void findActiveByEmail_userExists_returnsUser() {
        User user = new User();
        user.setEmail("owner@test.pl");
        when(userRepository.findByEmailAndActiveTrue("owner@test.pl")).thenReturn(Optional.of(user));

        User result = userService.findActiveByEmail("owner@test.pl");

        assertThat(result.getEmail()).isEqualTo("owner@test.pl");
    }

    @Test
    void findActiveByEmail_userNotFound_throwsUnauthorized() {
        when(userRepository.findByEmailAndActiveTrue("nobody@test.pl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findActiveByEmail("nobody@test.pl"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void findById_notFound_throwsUnauthorized() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(UnauthorizedException.class);
    }
}
