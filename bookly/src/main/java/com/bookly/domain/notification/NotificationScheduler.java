package com.bookly.domain.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processDueNotifications() {
        List<Notification> due = notificationRepository.findByStatusAndScheduledAtBefore(
                NotificationStatus.PENDING, LocalDateTime.now());

        for (Notification notification : due) {
            try {
                dispatch(notification);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                notification.setStatus(NotificationStatus.FAILED);
                log.error("Failed to send {} notification {}: {}",
                        notification.getType(), notification.getId(), e.getMessage());
            }
        }
    }

    private void dispatch(Notification notification) {
        switch (notification.getType()) {
            case EMAIL -> {
                if (notification.getRecipientEmail() == null) throw new IllegalStateException("No recipient email");
                emailService.sendAppointmentReminder(notification.getRecipientEmail(), notification.getAppointmentId());
                log.info("Email sent for appointment {}", notification.getAppointmentId());
            }
            case SMS -> {
                if (notification.getRecipientPhone() == null) throw new IllegalStateException("No recipient phone");
                smsService.sendAppointmentReminder(notification.getRecipientPhone(), notification.getAppointmentId());
                log.info("SMS sent for appointment {}", notification.getAppointmentId());
            }
        }
    }
}
