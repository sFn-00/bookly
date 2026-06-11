package com.bookly.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByAppointmentIdAndStatus(UUID appointmentId, NotificationStatus status);

    List<Notification> findByStatusAndScheduledAtBefore(NotificationStatus status, LocalDateTime now);
}
