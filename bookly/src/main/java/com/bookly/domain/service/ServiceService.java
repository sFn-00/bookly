package com.bookly.domain.service;

import com.bookly.api.service.dto.request.ServiceCreateRequest;
import com.bookly.api.service.dto.request.ServiceUpdateRequest;
import com.bookly.config.TenantContext;
import com.bookly.domain.appointment.AppointmentService;
import com.bookly.exception.ConflictException;
import com.bookly.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final AppointmentService appointmentService;

    @Transactional(readOnly = true)
    public List<com.bookly.domain.service.Service> findAll() {
        UUID tenantId = TenantContext.getTenantId();
        return serviceRepository.findAllByTenantIdAndActiveTrue(tenantId);
    }

    public com.bookly.domain.service.Service create(ServiceCreateRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        com.bookly.domain.service.Service service = new com.bookly.domain.service.Service();
        service.setTenantId(tenantId);
        service.setName(req.name());
        service.setDescription(req.description());
        service.setDurationMinutes(req.durationMinutes());
        service.setPrice(req.price());
        return serviceRepository.save(service);
    }

    public com.bookly.domain.service.Service update(UUID id, ServiceUpdateRequest req) {
        com.bookly.domain.service.Service service = findActiveForCurrentTenant(id);
        service.setName(req.name());
        service.setDescription(req.description());
        service.setDurationMinutes(req.durationMinutes());
        service.setPrice(req.price());
        return service;
    }

    public void softDelete(UUID id) {
        com.bookly.domain.service.Service service = findActiveForCurrentTenant(id);
        if (appointmentService.hasUpcomingAppointmentsForService(id)) {
            throw new ConflictException("Service has upcoming appointments");
        }
        service.setActive(false);
    }

    @Transactional(readOnly = true)
    public com.bookly.domain.service.Service findById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return serviceRepository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Service not found"));
    }

    private com.bookly.domain.service.Service findActiveForCurrentTenant(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return serviceRepository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Service not found"));
    }
}
