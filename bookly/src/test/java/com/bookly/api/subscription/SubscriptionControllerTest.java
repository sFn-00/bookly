package com.bookly.api.subscription;

import com.bookly.config.SecurityConfig;
import com.bookly.config.TenantContext;
import com.bookly.config.TenantFilter;
import com.bookly.domain.subscription.Subscription;
import com.bookly.domain.subscription.StripeService;
import com.bookly.domain.subscription.SubscriptionService;
import com.bookly.domain.subscription.SubscriptionStatus;
import com.bookly.domain.tenant.Plan;
import com.bookly.domain.tenant.Tenant;
import com.bookly.domain.tenant.TenantService;
import com.bookly.exception.NotFoundException;
import com.bookly.infrastructure.jwt.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
@Import(SecurityConfig.class)
class SubscriptionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SubscriptionService subscriptionService;
    @MockitoBean StripeService stripeService;
    @MockitoBean TenantService tenantService;
    @MockitoBean JwtService jwtService;
    @MockitoBean TenantFilter tenantFilter;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            TenantContext.setTenant(tenantId.toString());
            try {
                inv.getArgument(2, FilterChain.class).doFilter(inv.getArgument(0), inv.getArgument(1));
            } finally {
                TenantContext.clear();
            }
            return null;
        }).when(tenantFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void getCurrent_withSubscription_returns200() throws Exception {
        when(subscriptionService.findCurrentByTenantId(any()))
                .thenReturn(Optional.of(buildSubscription(Plan.PRO, SubscriptionStatus.ACTIVE)));

        mockMvc.perform(get("/api/subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("PRO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void getCurrent_noSubscription_returns404() throws Exception {
        when(subscriptionService.findCurrentByTenantId(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/subscription"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void upgrade_validPlan_returnsCheckoutUrl() throws Exception {
        when(stripeService.createCheckoutSession(any(), any()))
                .thenReturn("https://checkout.stripe.com/pay/cs_test_123");

        mockMvc.perform(post("/api/subscription/upgrade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"PRO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/pay/cs_test_123"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void cancel_withSubscription_returns204() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setStripeSubscriptionId("sub_123");
        when(tenantService.findById(any())).thenReturn(tenant);

        mockMvc.perform(post("/api/subscription/cancel"))
                .andExpect(status().isNoContent());

        verify(stripeService).cancelSubscription("sub_123");
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void cancel_noSubscription_returns404() throws Exception {
        Tenant tenant = new Tenant();
        when(tenantService.findById(any())).thenReturn(tenant);

        mockMvc.perform(post("/api/subscription/cancel"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getCurrent_asStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/subscription"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCurrent_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/subscription"))
                .andExpect(status().isForbidden());
    }

    private Subscription buildSubscription(Plan plan, SubscriptionStatus status) {
        Subscription s = new Subscription();
        s.setId(UUID.randomUUID());
        s.setTenantId(UUID.randomUUID());
        s.setPlan(plan);
        s.setStatus(status);
        return s;
    }
}
