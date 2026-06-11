package com.bookly.domain.appointment;

import com.bookly.config.TenantContext;
import com.bookly.domain.notification.NotificationService;
import com.bookly.exception.ConflictException;
import com.bookly.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock NotificationService notificationService;
    @InjectMocks AppointmentService appointmentService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenant(tenantId.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // --- findAll ---

    @Test
    void findAll_withStatus_queriesWithStatus() {
        AppointmentFilter filter = new AppointmentFilter(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), AppointmentStatus.PENDING);
        when(appointmentRepository.findByTenantIdAndStatusAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of(buildAppointment(AppointmentStatus.PENDING)));

        List<Appointment> result = appointmentService.findAll(filter);

        assertThat(result).hasSize(1);
        verify(appointmentRepository).findByTenantIdAndStatusAndStartTimeBetween(
                eq(tenantId), eq(AppointmentStatus.PENDING), any(), any());
    }

    @Test
    void findAll_withoutStatus_queriesAllStatuses() {
        AppointmentFilter filter = new AppointmentFilter(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), null);
        when(appointmentRepository.findByTenantIdAndStartTimeBetween(any(), any(), any()))
                .thenReturn(List.of());

        appointmentService.findAll(filter);

        verify(appointmentRepository).findByTenantIdAndStartTimeBetween(eq(tenantId), any(), any());
        verify(appointmentRepository, never()).findByTenantIdAndStatusAndStartTimeBetween(any(), any(), any(), any());
    }

    // --- confirm ---

    @Test
    void confirm_pendingAppointment_setsConfirmedAndCreatesNotifications() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(AppointmentStatus.PENDING);
        when(appointmentRepository.findByTenantIdAndStatusAndId(tenantId, AppointmentStatus.PENDING, id))
                .thenReturn(Optional.of(appointment));

        Appointment result = appointmentService.confirm(id);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(notificationService).createForAppointment(appointment);
    }

    @Test
    void confirm_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findByTenantIdAndStatusAndId(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.confirm(id))
                .isInstanceOf(NotFoundException.class);
        verify(notificationService, never()).createForAppointment(any());
    }

    // --- cancel ---

    @Test
    void cancel_existingAppointment_setsCancelledAndCancelsNotifications() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(AppointmentStatus.PENDING);
        when(appointmentRepository.findByTenantIdAndId(tenantId, id))
                .thenReturn(Optional.of(appointment));

        appointmentService.cancel(id);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(notificationService).cancelForAppointment(id);
    }

    @Test
    void cancel_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findByTenantIdAndId(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.cancel(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancel_alreadyCancelled_throwsConflictException() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findByTenantIdAndId(tenantId, id))
                .thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancel(id))
                .isInstanceOf(ConflictException.class);
        verify(notificationService, never()).cancelForAppointment(any());
    }

    private Appointment buildAppointment(AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setId(UUID.randomUUID());
        a.setTenantId(tenantId);
        a.setStaffId(UUID.randomUUID());
        a.setServiceId(UUID.randomUUID());
        a.setStartTime(LocalDateTime.of(2025, 1, 6, 10, 0));
        a.setEndTime(LocalDateTime.of(2025, 1, 6, 10, 30));
        a.setStatus(status);
        return a;
    }
}
