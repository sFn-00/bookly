package com.bookly.domain.user;

import com.bookly.api.auth.dto.LoginRequest;
import com.bookly.api.auth.dto.RegisterRequest;
import com.bookly.api.auth.dto.TokenPair;
import com.bookly.config.TenantContext;
import com.bookly.domain.tenant.Tenant;
import com.bookly.domain.tenant.TenantService;
import com.bookly.exception.UnauthorizedException;
import com.bookly.infrastructure.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final TenantService tenantService;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public TokenPair register(RegisterRequest req) {
        tenantService.assertSubdomainAvailable(req.subdomain());
        userService.assertEmailAvailable(req.email());

        Tenant tenant = tenantService.createTenant(req.companyName(), req.subdomain());
        User owner = userService.createOwner(
                tenant.getId(), req.email(), passwordEncoder.encode(req.password())
        );

        return new TokenPair(
                jwtService.generateAccessToken(owner, tenant.getId().toString()),
                jwtService.generateRefreshToken(owner)
        );
    }

    @Transactional(readOnly = true)
    public TokenPair login(LoginRequest req) {
        String tenantId = TenantContext.getTenant();
        User user = userService.findActiveByEmail(req.email());

        if (tenantId != null && !user.getTenantId().toString().equals(tenantId)) {
            throw new UnauthorizedException("invalid credentials");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }

        return new TokenPair(
                jwtService.generateAccessToken(user, user.getTenantId().toString()),
                jwtService.generateRefreshToken(user)
        );
    }

    @Transactional(readOnly = true)
    public TokenPair refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.validateRefreshToken(refreshToken);
        } catch (JwtException e) {
            throw new UnauthorizedException("invalid token");
        }

        User user = userService.findById(UUID.fromString(claims.getSubject()));

        String tenantId = TenantContext.getTenant();
        if (tenantId != null && !user.getTenantId().toString().equals(tenantId)) {
            throw new UnauthorizedException("invalid token");
        }

        return new TokenPair(
                jwtService.generateAccessToken(user, user.getTenantId().toString()),
                jwtService.generateRefreshToken(user)
        );
    }
}
