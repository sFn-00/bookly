package com.bookly.domain.notification;

import com.bookly.domain.appointment.Appointment;
import com.bookly.domain.tenant.Plan;
import com.bookly.domain.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TenantService tenantService;

    public void createForAppointment(Appointment appointment) {
        save(appointment.getId(), NotificationType.EMAIL, appointment.getStartTime().minusHours(24));

        Plan plan = tenantService.findById(appointment.getTenantId()).getPlan();
        if (plan == Plan.PRO || plan == Plan.ENTERPRISE) {
            save(appointment.getId(), NotificationType.SMS, appointment.getStartTime().minusHours(2));
        }
    }

    public void cancelForAppointment(UUID appointmentId) {
        notificationRepository.findByAppointmentIdAndStatus(appointmentId, NotificationStatus.PENDING)
                .forEach(n -> n.setStatus(NotificationStatus.CANCELLED));
    }

    private void save(UUID appointmentId, NotificationType type, LocalDateTime scheduledAt) {
        Notification n = new Notification();
        n.setAppointmentId(appointmentId);
        n.setType(type);
        n.setScheduledAt(scheduledAt);
        notificationRepository.save(n);
    }
}
