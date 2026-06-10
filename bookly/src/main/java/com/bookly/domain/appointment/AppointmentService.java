package com.bookly.domain.appointment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public boolean hasUpcomingAppointmentsForService(UUID serviceId) {
        return appointmentRepository.existsByServiceIdAndStatusNotAndStartTimeAfter(
                serviceId, "CANCELLED", LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public long countMonthlyBookings(UUID tenantId) {
        YearMonth current = YearMonth.now();
        LocalDateTime from = current.atDay(1).atStartOfDay();
        LocalDateTime to = current.atEndOfMonth().atTime(23, 59, 59);
        return appointmentRepository.countByTenantIdAndStatusNotAndStartTimeBetween(
                tenantId, "CANCELLED", from, to
        );
    }
}
