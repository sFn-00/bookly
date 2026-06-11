package com.bookly.domain.subscription;

import com.bookly.domain.tenant.Plan;
import com.bookly.domain.tenant.Tenant;
import com.bookly.domain.tenant.TenantService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final TenantService tenantService;

    @Value("${app.stripe.price-pro:}")
    private String pricePro;

    @Value("${app.stripe.price-enterprise:}")
    private String priceEnterprise;

    @Value("${app.stripe.success-url}")
    private String successUrl;

    @Value("${app.stripe.cancel-url}")
    private String cancelUrl;

    @Transactional
    public String createCheckoutSession(UUID tenantId, Plan plan) {
        try {
            Tenant tenant = tenantService.findById(tenantId);
            String customerId = resolveCustomer(tenant);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceIdFor(plan))
                            .setQuantity(1L)
                            .build())
                    .putMetadata("plan", plan.name())
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage(), e);
        }
    }

    public void cancelSubscription(String stripeSubscriptionId) {
        try {
            Subscription.retrieve(stripeSubscriptionId).cancel();
        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage(), e);
        }
    }

    private String resolveCustomer(Tenant tenant) throws StripeException {
        if (tenant.getStripeCustomerId() != null) {
            return tenant.getStripeCustomerId();
        }
        Customer customer = Customer.create(CustomerCreateParams.builder()
                .setName(tenant.getName())
                .putMetadata("tenantId", tenant.getId().toString())
                .build());
        tenant.setStripeCustomerId(customer.getId());
        return customer.getId();
    }

    private String priceIdFor(Plan plan) {
        return switch (plan) {
            case PRO -> pricePro;
            case ENTERPRISE -> priceEnterprise;
            case FREE -> throw new IllegalArgumentException("Cannot create checkout session for FREE plan");
        };
    }
}
