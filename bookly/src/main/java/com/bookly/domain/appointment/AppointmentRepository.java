package com.bookly.domain.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    boolean existsByServiceIdAndStatusNotAndStartTimeAfter(UUID serviceId, String status, LocalDateTime after);

    long countByTenantIdAndStatusNotAndStartTimeBetween(UUID tenantId, String status, LocalDateTime from, LocalDateTime to);
}
