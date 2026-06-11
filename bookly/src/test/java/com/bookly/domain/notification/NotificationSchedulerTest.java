package com.bookly.domain.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock NotificationRepository notificationRepository;
    @Mock EmailService emailService;
    @Mock SmsService smsService;
    @InjectMocks NotificationScheduler scheduler;

    @Test
    void processDueNotifications_emailNotification_sendsEmailAndMarksAsSent() {
        Notification n = buildNotification(NotificationType.EMAIL, "client@example.com", null);
        when(notificationRepository.findByStatusAndScheduledAtBefore(eq(NotificationStatus.PENDING), any()))
                .thenReturn(List.of(n));

        scheduler.processDueNotifications();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isNotNull();
        verify(emailService).sendAppointmentReminder("client@example.com", n.getAppointmentId());
        verify(smsService, never()).sendAppointmentReminder(any(), any());
    }

    @Test
    void processDueNotifications_smsNotification_sendsSmsAndMarksAsSent() {
        Notification n = buildNotification(NotificationType.SMS, null, "+48123456789");
        when(notificationRepository.findByStatusAndScheduledAtBefore(eq(NotificationStatus.PENDING), any()))
                .thenReturn(List.of(n));

        scheduler.processDueNotifications();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isNotNull();
        verify(smsService).sendAppointmentReminder("+48123456789", n.getAppointmentId());
        verify(emailService, never()).sendAppointmentReminder(any(), any());
    }

    @Test
    void processDueNotifications_sendFails_marksAsFailed() {
        Notification n = buildNotification(NotificationType.EMAIL, "client@example.com", null);
        when(notificationRepository.findByStatusAndScheduledAtBefore(any(), any()))
                .thenReturn(List.of(n));
        doThrow(new RuntimeException("SMTP error")).when(emailService).sendAppointmentReminder(any(), any());

        scheduler.processDueNotifications();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.getSentAt()).isNull();
    }

    @Test
    void processDueNotifications_noRecipientEmail_marksAsFailed() {
        Notification n = buildNotification(NotificationType.EMAIL, null, null);
        when(notificationRepository.findByStatusAndScheduledAtBefore(any(), any()))
                .thenReturn(List.of(n));

        scheduler.processDueNotifications();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(emailService, never()).sendAppointmentReminder(any(), any());
    }

    @Test
    void processDueNotifications_noRecipientPhone_marksAsFailed() {
        Notification n = buildNotification(NotificationType.SMS, null, null);
        when(notificationRepository.findByStatusAndScheduledAtBefore(any(), any()))
                .thenReturn(List.of(n));

        scheduler.processDueNotifications();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(smsService, never()).sendAppointmentReminder(any(), any());
    }

    @Test
    void processDueNotifications_noDueNotifications_doesNothing() {
        when(notificationRepository.findByStatusAndScheduledAtBefore(any(), any()))
                .thenReturn(List.of());

        scheduler.processDueNotifications();

        verify(emailService, never()).sendAppointmentReminder(any(), any());
        verify(smsService, never()).sendAppointmentReminder(any(), any());
    }

    private Notification buildNotification(NotificationType type, String email, String phone) {
        Notification n = new Notification();
        n.setId(UUID.randomUUID());
        n.setAppointmentId(UUID.randomUUID());
        n.setType(type);
        n.setStatus(NotificationStatus.PENDING);
        n.setScheduledAt(LocalDateTime.now().minusMinutes(5));
        n.setRecipientEmail(email);
        n.setRecipientPhone(phone);
        return n;
    }
}
