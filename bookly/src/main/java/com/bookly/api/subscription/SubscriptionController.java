package com.bookly.api.subscription;

import com.bookly.api.subscription.dto.SubscriptionDTO;
import com.bookly.api.subscription.dto.UpgradeRequest;
import com.bookly.config.Roles;
import com.bookly.config.TenantContext;
import com.bookly.domain.subscription.StripeService;
import com.bookly.domain.subscription.SubscriptionService;
import com.bookly.domain.tenant.Tenant;
import com.bookly.domain.tenant.TenantService;
import com.bookly.exception.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@PreAuthorize(Roles.OWNER_ONLY)
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final StripeService stripeService;
    private final TenantService tenantService;

    @GetMapping
    public ResponseEntity<SubscriptionDTO> getCurrent() {
        return subscriptionService.findCurrentByTenantId(TenantContext.getTenantId())
                .map(s -> ResponseEntity.ok(SubscriptionDTO.from(s)))
                .orElseThrow(() -> new NotFoundException("No active subscription"));
    }

    @PostMapping("/upgrade")
    public ResponseEntity<Map<String, String>> upgrade(@Valid @RequestBody UpgradeRequest request) {
        String checkoutUrl = stripeService.createCheckoutSession(TenantContext.getTenantId(), request.plan());
        return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel() {
        Tenant tenant = tenantService.findById(TenantContext.getTenantId());
        if (tenant.getStripeSubscriptionId() == null) {
            throw new NotFoundException("No active subscription to cancel");
        }
        stripeService.cancelSubscription(tenant.getStripeSubscriptionId());
        return ResponseEntity.noContent().build();
    }
}
