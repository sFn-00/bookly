package com.bookly.domain.user;

import com.bookly.exception.ConflictException;
import com.bookly.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void assertEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("email already registered");
        }
    }

    @Transactional
    public User createOwner(UUID tenantId, String email, String encodedPassword) {
        User owner = new User();
        owner.setTenantId(tenantId);
        owner.setEmail(email);
        owner.setPasswordHash(encodedPassword);
        owner.setRole(UserRole.OWNER);
        return userRepository.save(owner);
    }

    @Transactional(readOnly = true)
    public User findActiveByEmail(String email) {
        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new UnauthorizedException("invalid credentials"));
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException("invalid token"));
    }
}
