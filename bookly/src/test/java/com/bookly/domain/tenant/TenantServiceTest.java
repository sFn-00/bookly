package com.bookly.domain.tenant;

import com.bookly.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock TenantRepository tenantRepository;
    @InjectMocks TenantService tenantService;

    @Test
    void assertSubdomainAvailable_subdomainTaken_throwsConflict() {
        when(tenantRepository.existsBySubdomain("taken")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.assertSubdomainAvailable("taken"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("subdomain already taken");
    }

    @Test
    void assertSubdomainAvailable_subdomainFree_doesNotThrow() {
        when(tenantRepository.existsBySubdomain("free")).thenReturn(false);

        tenantService.assertSubdomainAvailable("free");
    }

    @Test
    void createTenant_savesWithCorrectFields() {
        Tenant saved = new Tenant();
        saved.setId(UUID.randomUUID());
        saved.setName("Hair Salon");
        saved.setSubdomain("hairsalon");
        when(tenantRepository.save(any())).thenReturn(saved);

        Tenant result = tenantService.createTenant("Hair Salon", "hairsalon");

        assertThat(result.getName()).isEqualTo("Hair Salon");
        assertThat(result.getSubdomain()).isEqualTo("hairsalon");
    }

    @Test
    void findBySubdomain_tenantExists_returnsTenant() {
        Tenant tenant = new Tenant();
        tenant.setSubdomain("hairsalon");
        when(tenantRepository.findBySubdomain("hairsalon")).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.findBySubdomain("hairsalon");

        assertThat(result.getSubdomain()).isEqualTo("hairsalon");
    }

    @Test
    void findBySubdomain_notFound_throwsIllegalArgument() {
        when(tenantRepository.findBySubdomain("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.findBySubdomain("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatePlan_tenantExists_updatesPlan() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setPlan(Plan.FREE);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        tenantService.updatePlan(tenantId, Plan.PRO);

        assertThat(tenant.getPlan()).isEqualTo(Plan.PRO);
    }
}
