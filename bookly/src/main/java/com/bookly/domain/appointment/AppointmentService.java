package com.bookly.domain.appointment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
}
