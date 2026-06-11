package com.bookly.api.subscription.dto;

import com.bookly.domain.subscription.Subscription;
import com.bookly.domain.subscription.SubscriptionStatus;
import com.bookly.domain.tenant.Plan;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionDTO(
        UUID id,
        Plan plan,
        SubscriptionStatus status,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd
) {
    public static SubscriptionDTO from(Subscription s) {
        return new SubscriptionDTO(s.getId(), s.getPlan(), s.getStatus(),
                s.getCurrentPeriodStart(), s.getCurrentPeriodEnd());
    }
}
