package com.bookly.domain.subscription;

import com.bookly.domain.tenant.Plan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public void create(UUID tenantId, String stripeSubscriptionId, String stripeCustomerId, Plan plan) {
        Subscription subscription = new Subscription();
        subscription.setTenantId(tenantId);
        subscription.setStripeSubscriptionId(stripeSubscriptionId);
        subscription.setStripeCustomerId(stripeCustomerId);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);
    }

    public void cancel(String stripeSubscriptionId) {
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .ifPresent(s -> s.setStatus(SubscriptionStatus.CANCELLED));
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> findCurrentByTenantId(UUID tenantId) {
        return subscriptionRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId);
    }
}
