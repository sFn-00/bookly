package com.bookly.domain.user;

import com.bookly.api.auth.dto.LoginRequest;
import com.bookly.api.auth.dto.RegisterRequest;
import com.bookly.api.auth.dto.TokenPair;
import com.bookly.config.TenantContext;
import com.bookly.domain.tenant.Tenant;
import com.bookly.domain.tenant.TenantService;
import com.bookly.exception.ConflictException;
import com.bookly.exception.UnauthorizedException;
import com.bookly.infrastructure.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock TenantService tenantService;
    @Mock UserService userService;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setSubdomain("hairsalon");

        user = new User();
        user.setId(UUID.randomUUID());
        user.setTenantId(tenant.getId());
        user.setEmail("owner@hairsalon.pl");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.OWNER);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void register_createsTenantsAndUserAndReturnsTokens() {
        RegisterRequest req = new RegisterRequest("Hair Salon", "hairsalon", "owner@hairsalon.pl", "Password1!");
        when(tenantService.createTenant("Hair Salon", "hairsalon")).thenReturn(tenant);
        when(userService.createOwner(any(), anyString(), anyString())).thenReturn(user);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed");
        when(jwtService.generateAccessToken(user, tenant.getId().toString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        TokenPair result = authService.register(req);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(tenantService).assertSubdomainAvailable("hairsalon");
        verify(userService).assertEmailAvailable("owner@hairsalon.pl");
    }

    @Test
    void register_subdomainTaken_throwsConflict() {
        RegisterRequest req = new RegisterRequest("Hair Salon", "taken", "owner@hairsalon.pl", "Password1!");
        doThrow(new ConflictException("subdomain already taken"))
                .when(tenantService).assertSubdomainAvailable("taken");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessage("subdomain already taken");
    }

    @Test
    void register_emailTaken_throwsConflict() {
        RegisterRequest req = new RegisterRequest("Hair Salon", "hairsalon", "taken@email.pl", "Password1!");
        doThrow(new ConflictException("email already registered"))
                .when(userService).assertEmailAvailable("taken@email.pl");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessage("email already registered");
    }

    @Test
    void login_validCredentials_returnsTokens() {
        TenantContext.setTenant(tenant.getId().toString());
        LoginRequest req = new LoginRequest("owner@hairsalon.pl", "Password1!");
        when(userService.findActiveByEmail("owner@hairsalon.pl")).thenReturn(user);
        when(passwordEncoder.matches("Password1!", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user, tenant.getId().toString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        TokenPair result = authService.login(req);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        TenantContext.setTenant(tenant.getId().toString());
        LoginRequest req = new LoginRequest("owner@hairsalon.pl", "wrong");
        when(userService.findActiveByEmail("owner@hairsalon.pl")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_tenantMismatch_throwsUnauthorized() {
        TenantContext.setTenant(UUID.randomUUID().toString()); // different tenant
        LoginRequest req = new LoginRequest("owner@hairsalon.pl", "Password1!");
        when(userService.findActiveByEmail("owner@hairsalon.pl")).thenReturn(user);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_validToken_returnsNewTokens() {
        TenantContext.setTenant(tenant.getId().toString());
        Claims claims = new DefaultClaims(Map.of("sub", user.getId().toString()));
        when(jwtService.validateRefreshToken("refresh-token")).thenReturn(claims);
        when(userService.findById(user.getId())).thenReturn(user);
        when(jwtService.generateAccessToken(user, tenant.getId().toString())).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");

        TokenPair result = authService.refresh("refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_tenantMismatch_throwsUnauthorized() {
        TenantContext.setTenant(UUID.randomUUID().toString()); // different tenant
        Claims claims = new DefaultClaims(Map.of("sub", user.getId().toString()));
        when(jwtService.validateRefreshToken("refresh-token")).thenReturn(claims);
        when(userService.findById(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
