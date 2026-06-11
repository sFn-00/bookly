package com.bookly.domain.appointment;

import com.bookly.config.TenantContext;
import com.bookly.domain.notification.NotificationService;
import com.bookly.exception.ConflictException;
import com.bookly.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public boolean hasUpcomingAppointmentsForService(UUID serviceId) {
        return appointmentRepository.existsByServiceIdAndStatusNotAndStartTimeAfter(
                serviceId, AppointmentStatus.CANCELLED, LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public long countMonthlyBookings(UUID tenantId) {
        YearMonth current = YearMonth.now();
        LocalDateTime from = current.atDay(1).atStartOfDay();
        LocalDateTime to = current.atEndOfMonth().atTime(23, 59, 59);
        return appointmentRepository.countByTenantIdAndStatusNotAndStartTimeBetween(
                tenantId, AppointmentStatus.CANCELLED, from, to
        );
    }

    @Transactional(readOnly = true)
    public List<Appointment> findAll(AppointmentFilter filter) {
        UUID tenantId = TenantContext.getTenantId();
        LocalDateTime from = filter.from().atStartOfDay();
        LocalDateTime to = filter.to().atTime(23, 59, 59);

        if (filter.status() == null) {
            return appointmentRepository.findByTenantIdAndStartTimeBetween(tenantId, from, to);
        }
        return appointmentRepository.findByTenantIdAndStatusAndStartTimeBetween(tenantId, filter.status(), from, to);
    }

    public Appointment confirm(UUID id) {
        Appointment appointment = appointmentRepository
                .findByTenantIdAndStatusAndId(TenantContext.getTenantId(), AppointmentStatus.PENDING, id)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        notificationService.createForAppointment(appointment);
        return appointment;
    }

    public void cancel(UUID id) {
        Appointment appointment = appointmentRepository
                .findByTenantIdAndId(TenantContext.getTenantId(), id)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("Appointment is already cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        notificationService.cancelForAppointment(id);
    }
}
