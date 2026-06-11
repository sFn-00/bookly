package com.bookly.domain.notification;

import com.bookly.domain.appointment.Appointment;
import com.bookly.domain.client.Client;
import com.bookly.domain.client.ClientService;
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
    private final ClientService clientService;

    public void createForAppointment(Appointment appointment) {
        String email = resolveEmail(appointment);
        String phone = resolvePhone(appointment);

        save(appointment.getId(), NotificationType.EMAIL, appointment.getStartTime().minusHours(24), email, null);

        Plan plan = tenantService.findById(appointment.getTenantId()).getPlan();
        if (plan == Plan.PRO || plan == Plan.ENTERPRISE) {
            save(appointment.getId(), NotificationType.SMS, appointment.getStartTime().minusHours(2), null, phone);
        }
    }

    public void cancelForAppointment(UUID appointmentId) {
        notificationRepository.findByAppointmentIdAndStatus(appointmentId, NotificationStatus.PENDING)
                .forEach(n -> n.setStatus(NotificationStatus.CANCELLED));
    }

    private String resolveEmail(Appointment appointment) {
        if (appointment.getClientId() == null) return null;
        return clientService.findById(appointment.getClientId())
                .map(Client::getEmail)
                .orElse(null);
    }

    private String resolvePhone(Appointment appointment) {
        if (appointment.getClientId() == null) return null;
        return clientService.findById(appointment.getClientId())
                .map(Client::getPhone)
                .orElse(null);
    }

    private void save(UUID appointmentId, NotificationType type, LocalDateTime scheduledAt,
                      String recipientEmail, String recipientPhone) {
        Notification n = new Notification();
        n.setAppointmentId(appointmentId);
        n.setType(type);
        n.setScheduledAt(scheduledAt);
        n.setRecipientEmail(recipientEmail);
        n.setRecipientPhone(recipientPhone);
        notificationRepository.save(n);
    }
}
