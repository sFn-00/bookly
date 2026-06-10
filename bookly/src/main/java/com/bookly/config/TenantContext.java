package com.bookly.config;

import com.bookly.exception.UnauthorizedException;

import java.util.UUID;

public class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setTenant(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }

    public static UUID getTenantId() {
        return UUID.fromString(getTenant());
    }

    public static void assertTenantMatch(UUID entityTenantId) {
        String contextTenantId = getTenant();
        if (contextTenantId != null && !entityTenantId.toString().equals(contextTenantId)) {
            throw new UnauthorizedException("tenant mismatch");
        }
    }
}
