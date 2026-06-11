package com.bookly.domain.subscription;

import com.bookly.domain.tenant.Plan;
import com.bookly.domain.tenant.Tenant;
import com.bookly.domain.tenant.TenantService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionEventHandlerTest {

    @Mock TenantService tenantService;
    @Mock SubscriptionService subscriptionService;
    @InjectMocks SubscriptionEventHandler handler;

    // --- checkout.session.completed ---

    @Test
    void onCheckoutSessionCompleted_validEvent_upgradesTenantAndCreatesSubscription() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = buildTenant(tenantId, null);
        Event event = buildSessionEvent("cus_123", "sub_123", "PRO");

        when(tenantService.findByStripeCustomerId("cus_123")).thenReturn(tenant);

        handler.onCheckoutSessionCompleted(event);

        verify(tenantService).updatePlan(tenantId, Plan.PRO);
        verify(subscriptionService).create(tenantId, "sub_123", "cus_123", Plan.PRO);
    }

    @Test
    void onCheckoutSessionCompleted_missingPlanMetadata_skips() {
        Event event = buildSessionEvent("cus_123", "sub_123", null);

        handler.onCheckoutSessionCompleted(event);

        verify(tenantService, never()).updatePlan(any(), any());
        verify(subscriptionService, never()).create(any(), any(), any(), any());
    }

    // --- customer.subscription.deleted ---

    @Test
    void onSubscriptionDeleted_validEvent_cancelsAndDowngradesToFree() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = buildTenant(tenantId, "sub_123");
        Event event = buildSubscriptionEvent("sub_123", "cus_123");

        when(tenantService.findByStripeCustomerId("cus_123")).thenReturn(tenant);

        handler.onSubscriptionDeleted(event);

        verify(subscriptionService).cancel("sub_123");
        verify(tenantService).updatePlan(tenantId, Plan.FREE);
    }

    // --- helpers ---

    private Event buildSessionEvent(String customerId, String subscriptionId, String plan) {
        Session session = mock(Session.class);
        when(session.getCustomer()).thenReturn(customerId);
        when(session.getSubscription()).thenReturn(subscriptionId);
        when(session.getMetadata()).thenReturn(plan != null ? Map.of("plan", plan) : Map.of());

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        Event event = mock(Event.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }

    private Event buildSubscriptionEvent(String subscriptionId, String customerId) {
        com.stripe.model.Subscription subscription = mock(com.stripe.model.Subscription.class);
        when(subscription.getId()).thenReturn(subscriptionId);
        when(subscription.getCustomer()).thenReturn(customerId);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(subscription));

        Event event = mock(Event.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }

    private Tenant buildTenant(UUID id, String stripeSubscriptionId) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setName("Test");
        t.setSubdomain("test");
        t.setStripeSubscriptionId(stripeSubscriptionId);
        return t;
    }
}
