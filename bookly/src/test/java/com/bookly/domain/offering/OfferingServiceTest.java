package com.bookly.domain.offering;

import com.bookly.api.offering.dto.request.OfferingCreateRequest;
import com.bookly.api.offering.dto.request.OfferingUpdateRequest;
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
class OfferingServiceTest {

    @Mock OfferingRepository offeringRepository;
    @Mock AppointmentService appointmentService;
    @InjectMocks OfferingService offeringService;

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
    void findAll_returnsActiveOfferingsForCurrentTenant() {
        Offering s1 = buildOffering("Haircut", tenantId);
        Offering s2 = buildOffering("Shave", tenantId);
        when(offeringRepository.findAllByTenantIdAndActiveTrue(tenantId)).thenReturn(List.of(s1, s2));

        List<Offering> result = offeringService.findAll();

        assertThat(result).hasSize(2);
        verify(offeringRepository).findAllByTenantIdAndActiveTrue(tenantId);
    }

    @Test
    void create_savesOfferingWithTenantId() {
        OfferingCreateRequest req = new OfferingCreateRequest("Haircut", "desc", 30, BigDecimal.valueOf(50));
        Offering saved = buildOffering("Haircut", tenantId);
        when(offeringRepository.save(any())).thenReturn(saved);

        Offering result = offeringService.create(req);

        assertThat(result.getName()).isEqualTo("Haircut");
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        verify(offeringRepository).save(any());
    }

    @Test
    void update_modifiesExistingOffering() {
        UUID offeringId = UUID.randomUUID();
        Offering existing = buildOffering("Old Name", tenantId);
        when(offeringRepository.findByIdAndTenantIdAndActiveTrue(offeringId, tenantId)).thenReturn(Optional.of(existing));
        OfferingUpdateRequest req = new OfferingUpdateRequest("New Name", "new desc", 60, BigDecimal.valueOf(100));

        Offering result = offeringService.update(offeringId, req);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDurationMinutes()).isEqualTo(60);
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        UUID offeringId = UUID.randomUUID();
        when(offeringRepository.findByIdAndTenantIdAndActiveTrue(offeringId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offeringService.update(offeringId,
                new OfferingUpdateRequest("x", null, 30, BigDecimal.ONE)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void softDelete_noUpcomingAppointments_deactivatesOffering() {
        UUID offeringId = UUID.randomUUID();
        Offering offering = buildOffering("Haircut", tenantId);
        when(offeringRepository.findByIdAndTenantIdAndActiveTrue(offeringId, tenantId)).thenReturn(Optional.of(offering));
        when(appointmentService.hasUpcomingAppointmentsForOffering(offeringId)).thenReturn(false);

        offeringService.softDelete(offeringId);

        assertThat(offering.isActive()).isFalse();
    }

    @Test
    void softDelete_hasUpcomingAppointments_throwsConflict() {
        UUID offeringId = UUID.randomUUID();
        Offering offering = buildOffering("Haircut", tenantId);
        when(offeringRepository.findByIdAndTenantIdAndActiveTrue(offeringId, tenantId)).thenReturn(Optional.of(offering));
        when(appointmentService.hasUpcomingAppointmentsForOffering(offeringId)).thenReturn(true);

        assertThatThrownBy(() -> offeringService.softDelete(offeringId))
                .isInstanceOf(ConflictException.class);
        assertThat(offering.isActive()).isTrue();
    }

    @Test
    void softDelete_notFound_throwsNotFoundException() {
        UUID offeringId = UUID.randomUUID();
        when(offeringRepository.findByIdAndTenantIdAndActiveTrue(offeringId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offeringService.softDelete(offeringId))
                .isInstanceOf(NotFoundException.class);
    }

    private Offering buildOffering(String name, UUID tenantId) {
        Offering s = new Offering();
        s.setId(UUID.randomUUID());
        s.setTenantId(tenantId);
        s.setName(name);
        s.setDurationMinutes(30);
        s.setPrice(BigDecimal.valueOf(50));
        s.setActive(true);
        return s;
    }
}
