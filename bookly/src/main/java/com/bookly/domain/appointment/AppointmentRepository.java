package com.bookly.domain.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByTenantIdAndStatusAndId(UUID tenantId, AppointmentStatus status, UUID uuid);

    Optional<Appointment> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByServiceIdAndStatusNotAndStartTimeAfter(
            UUID serviceId, AppointmentStatus status, LocalDateTime after);

    long countByTenantIdAndStatusNotAndStartTimeBetween(
            UUID tenantId, AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByStaffIdAndStatusInAndStartTimeBetween(
            UUID staffId, List<AppointmentStatus> statuses, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByTenantIdAndStartTimeBetween(
            UUID tenantId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByTenantIdAndStatusAndStartTimeBetween(
            UUID tenantId, AppointmentStatus status, LocalDateTime from, LocalDateTime to);
}
