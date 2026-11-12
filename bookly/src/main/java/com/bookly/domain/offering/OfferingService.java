package com.bookly.domain.offering;

import com.bookly.api.offering.dto.request.OfferingCreateRequest;
import com.bookly.api.offering.dto.request.OfferingUpdateRequest;
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
public class OfferingService {

    private final OfferingRepository offeringRepository;
    private final AppointmentService appointmentService;

    @Transactional(readOnly = true)
    public List<Offering> findAll() {
        UUID tenantId = TenantContext.getTenantId();
        return offeringRepository.findAllByTenantIdAndActiveTrue(tenantId);
    }

    public Offering create(OfferingCreateRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        Offering offering = new Offering();
        offering.setTenantId(tenantId);
        offering.setName(req.name());
        offering.setDescription(req.description());
        offering.setDurationMinutes(req.durationMinutes());
        offering.setPrice(req.price());
        return offeringRepository.save(offering);
    }

    public Offering update(UUID id, OfferingUpdateRequest req) {
        Offering offering = findActiveForCurrentTenant(id);
        offering.setName(req.name());
        offering.setDescription(req.description());
        offering.setDurationMinutes(req.durationMinutes());
        offering.setPrice(req.price());
        return offering;
    }

    public void softDelete(UUID id) {
        Offering offering = findActiveForCurrentTenant(id);
        if (appointmentService.hasUpcomingAppointmentsForOffering(id)) {
            throw new ConflictException("Offering has upcoming appointments");
        }
        offering.setActive(false);
    }

    @Transactional(readOnly = true)
    public Offering findById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return offeringRepository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Offering not found"));
    }

    private Offering findActiveForCurrentTenant(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return offeringRepository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Offering not found"));
    }
}
