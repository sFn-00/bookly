package com.bookly.config;

import com.bookly.domain.tenant.Tenant;
import com.bookly.domain.tenant.TenantRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

    @Mock
    TenantRepository tenantRepository;

    TenantFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantFilter(tenantRepository);
    }

    @Test
    void unknownSubdomain_returns404() throws Exception {
        when(tenantRepository.findBySubdomainAndActiveTrue("unknown")).thenReturn(Optional.empty());

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Subdomain", "unknown");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(404);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void validSubdomain_setsTenantContextAndClearsAfterRequest() throws Exception {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(tenantRepository.findBySubdomainAndActiveTrue("hairsalon")).thenReturn(Optional.of(tenant));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Subdomain", "hairsalon");
        MockHttpServletResponse res = new MockHttpServletResponse();

        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        FilterChain chain = (request, response) -> capturedTenantId.set(TenantContext.getTenant());

        filter.doFilter(req, res, chain);

        assertThat(capturedTenantId.get()).isEqualTo(tenantId.toString());
        assertThat(TenantContext.getTenant()).isNull(); // cleared in finally block
    }

    @Test
    void tenantFreePath_skipsTenantResolutionAndCallsChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/auth/register");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        verifyNoInteractions(tenantRepository);
    }

    @Test
    void noSubdomain_returns404() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        // no X-Subdomain header, serverName = "localhost"
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(404);
        verify(chain, never()).doFilter(any(), any());
        verifyNoInteractions(tenantRepository);
    }
}
