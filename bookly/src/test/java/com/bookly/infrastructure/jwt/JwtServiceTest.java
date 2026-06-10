package com.bookly.infrastructure.jwt;

import com.bookly.domain.user.User;
import com.bookly.domain.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;
    private String tenantId;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "test-secret-key-must-be-at-least-256-bits-long-for-hmac!!");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L);
        ReflectionTestUtils.invokeMethod(jwtService, "initKey");

        user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.OWNER);

        tenantId = UUID.randomUUID().toString();
    }

    @Test
    void generateAccessToken_containsExpectedClaims() {
        String token = jwtService.generateAccessToken(user, tenantId);

        Claims claims = jwtService.validateToken(token);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("tenantId", String.class)).isEqualTo(tenantId);
        assertThat(claims.get("role", String.class)).isEqualTo("OWNER");
        assertThat(claims.get("type", String.class)).isNull();
    }

    @Test
    void generateRefreshToken_containsTypeRefreshClaim() {
        String token = jwtService.generateRefreshToken(user);

        Claims claims = jwtService.validateToken(token);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("type", String.class)).isEqualTo("REFRESH");
        assertThat(claims.get("role", String.class)).isNull();
    }

    @Test
    void validateRefreshToken_acceptsValidRefreshToken() {
        String token = jwtService.generateRefreshToken(user);

        Claims claims = jwtService.validateRefreshToken(token);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
    }

    @Test
    void validateRefreshToken_rejectsAccessToken() {
        String accessToken = jwtService.generateAccessToken(user, tenantId);

        assertThatThrownBy(() -> jwtService.validateRefreshToken(accessToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateToken_rejectsExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L);

        String token = jwtService.generateAccessToken(user, tenantId);

        assertThatThrownBy(() -> jwtService.validateToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateToken_rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(user, tenantId);
        String tampered = token.substring(0, token.length() - 5) + "AAAAA";

        assertThatThrownBy(() -> jwtService.validateToken(tampered))
                .isInstanceOf(JwtException.class);
    }
}
