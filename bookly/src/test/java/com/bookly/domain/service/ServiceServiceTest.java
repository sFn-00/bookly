package com.bookly.domain.service;

import com.bookly.api.service.dto.request.ServiceCreateRequest;
import com.bookly.api.service.dto.request.ServiceUpdateRequest;
import com.bookly.config.TenantContext;
import com.bookly.domain.appointment.AppointmentService;
import com.bookly.exception.ConflictException;
import com.bookly.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock ServiceRepository serviceRepository;
    @Mock AppointmentService appointmentService;
    @InjectMocks ServiceService serviceService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenant(tenantId.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findAll_returnsActiveServicesForCurrentTenant() {
        Service s1 = buildService("Haircut", tenantId);
        Service s2 = buildService("Shave", tenantId);
        when(serviceRepository.findAllByTenantIdAndActiveTrue(tenantId)).thenReturn(List.of(s1, s2));

        List<Service> result = serviceService.findAll();

        assertThat(result).hasSize(2);
        verify(serviceRepository).findAllByTenantIdAndActiveTrue(tenantId);
    }

    @Test
    void create_savesServiceWithTenantId() {
        ServiceCreateRequest req = new ServiceCreateRequest("Haircut", "desc", 30, BigDecimal.valueOf(50));
        Service saved = buildService("Haircut", tenantId);
        when(serviceRepository.save(any())).thenReturn(saved);

        Service result = serviceService.create(req);

        assertThat(result.getName()).isEqualTo("Haircut");
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        verify(serviceRepository).save(any());
    }

    @Test
    void update_modifiesExistingService() {
        UUID serviceId = UUID.randomUUID();
        Service existing = buildService("Old Name", tenantId);
        when(serviceRepository.findByIdAndTenantIdAndActiveTrue(serviceId, tenantId)).thenReturn(Optional.of(existing));
        ServiceUpdateRequest req = new ServiceUpdateRequest("New Name", "new desc", 60, BigDecimal.valueOf(100));

        Service result = serviceService.update(serviceId, req);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDurationMinutes()).isEqualTo(60);
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        UUID serviceId = UUID.randomUUID();
        when(serviceRepository.findByIdAndTenantIdAndActiveTrue(serviceId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceService.update(serviceId,
                new ServiceUpdateRequest("x", null, 30, BigDecimal.ONE)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void softDelete_noUpcomingAppointments_deactivatesService() {
        UUID serviceId = UUID.randomUUID();
        Service service = buildService("Haircut", tenantId);
        when(serviceRepository.findByIdAndTenantIdAndActiveTrue(serviceId, tenantId)).thenReturn(Optional.of(service));
        when(appointmentService.hasUpcomingAppointmentsForService(serviceId)).thenReturn(false);

        serviceService.softDelete(serviceId);

        assertThat(service.isActive()).isFalse();
    }

    @Test
    void softDelete_hasUpcomingAppointments_throwsConflict() {
        UUID serviceId = UUID.randomUUID();
        Service service = buildService("Haircut", tenantId);
        when(serviceRepository.findByIdAndTenantIdAndActiveTrue(serviceId, tenantId)).thenReturn(Optional.of(service));
        when(appointmentService.hasUpcomingAppointmentsForService(serviceId)).thenReturn(true);

        assertThatThrownBy(() -> serviceService.softDelete(serviceId))
                .isInstanceOf(ConflictException.class);
        assertThat(service.isActive()).isTrue();
    }

    @Test
    void softDelete_notFound_throwsNotFoundException() {
        UUID serviceId = UUID.randomUUID();
        when(serviceRepository.findByIdAndTenantIdAndActiveTrue(serviceId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceService.softDelete(serviceId))
                .isInstanceOf(NotFoundException.class);
    }

    private Service buildService(String name, UUID tenantId) {
        Service s = new Service();
        s.setId(UUID.randomUUID());
        s.setTenantId(tenantId);
        s.setName(name);
        s.setDurationMinutes(30);
        s.setPrice(BigDecimal.valueOf(50));
        s.setActive(true);
        return s;
    }
}
