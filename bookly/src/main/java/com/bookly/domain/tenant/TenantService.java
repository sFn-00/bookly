package com.bookly.domain.tenant;

import com.bookly.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    public void assertSubdomainAvailable(String subdomain) {
        if (tenantRepository.existsBySubdomain(subdomain)) {
            throw new ConflictException("subdomain already taken");
        }
    }

    @Transactional
    public Tenant createTenant(String name, String subdomain) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setSubdomain(subdomain);
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant findBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + subdomain));
    }

    @Transactional(readOnly = true)
    public Tenant findById(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    }

    @Transactional(readOnly = true)
    public Tenant findByStripeCustomerId(String stripeCustomerId) {
        return tenantRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseThrow(() -> new IllegalStateException("No tenant for customer " + stripeCustomerId));
    }

    @Transactional
    public void updatePlan(UUID tenantId, Plan plan) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        tenant.setPlan(plan);
    }
}
