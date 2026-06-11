package com.bookly.domain.subscription;

import com.bookly.domain.tenant.Plan;
import com.bookly.domain.tenant.Tenant;
import com.bookly.domain.tenant.TenantService;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventHandler {

    private final TenantService tenantService;
    private final SubscriptionService subscriptionService;

    @Transactional
    public void onCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("Cannot deserialize checkout session"));

        String customerId = session.getCustomer();
        String stripeSubscriptionId = session.getSubscription();
        String planName = session.getMetadata().get("plan");

        if (planName == null) {
            log.warn("checkout.session.completed missing 'plan' metadata — skipping");
            return;
        }

        Plan plan = Plan.valueOf(planName);
        Tenant tenant = tenantService.findByStripeCustomerId(customerId);

        tenantService.updatePlan(tenant.getId(), plan);
        tenant.setStripeSubscriptionId(stripeSubscriptionId);

        subscriptionService.create(tenant.getId(), stripeSubscriptionId, customerId, plan);

        log.info("Tenant {} upgraded to plan {}", tenant.getId(), plan);
    }

    @Transactional
    public void onSubscriptionDeleted(Event event) {
        Subscription stripeSubscription = (Subscription) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("Cannot deserialize subscription"));

        String stripeSubscriptionId = stripeSubscription.getId();
        String customerId = stripeSubscription.getCustomer();

        subscriptionService.cancel(stripeSubscriptionId);

        Tenant tenant = tenantService.findByStripeCustomerId(customerId);
        tenantService.updatePlan(tenant.getId(), Plan.FREE);

        log.info("Tenant {} downgraded to FREE (subscription {} deleted)", tenant.getId(), stripeSubscriptionId);
    }
}
