package com.bookly.config;

import com.bookly.domain.tenant.TenantRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantFilter implements Filter {

    // Paths that don't belong to any specific tenant
    private static final Set<String> TENANT_FREE_PATHS = Set.of(
            "/api/auth/register",
            "/api/stripe/webhook"
    );

    private final TenantRepository tenantRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        if (isTenantFreePath(httpReq.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String subdomain = extractSubdomain(httpReq);
        if (subdomain == null || subdomain.isBlank()) {
            httpRes.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        var tenant = tenantRepository.findBySubdomainAndActiveTrue(subdomain);
        if (tenant.isEmpty()) {
            httpRes.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            TenantContext.setTenant(tenant.get().getId().toString());
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isTenantFreePath(String uri) {
        return TENANT_FREE_PATHS.stream().anyMatch(uri::startsWith);
    }

    private String extractSubdomain(HttpServletRequest request) {
        // Local dev / testing: explicit header (no Nginx in front)
        String xSubdomain = request.getHeader("X-Subdomain");
        if (xSubdomain != null && !xSubdomain.isBlank()) {
            return xSubdomain.trim().toLowerCase();
        }

        // Production: Nginx sets Host = hairsalon.bookly.pl
        String host = request.getServerName();
        if (host != null && host.endsWith(".bookly.pl")) {
            return host.substring(0, host.length() - ".bookly.pl".length()).toLowerCase();
        }

        return null;
    }
}
