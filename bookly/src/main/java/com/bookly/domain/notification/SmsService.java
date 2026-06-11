package com.bookly.domain.notification;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class SmsService {

    @Value("${app.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.twilio.auth-token:}")
    private String authToken;

    @Value("${app.twilio.from-number:}")
    private String fromNumber;

    private boolean enabled;

    @PostConstruct
    void init() {
        if (!accountSid.isBlank() && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            enabled = true;
        } else {
            log.warn("Twilio credentials not configured — SMS sending disabled");
        }
    }

    public void sendAppointmentReminder(String to, UUID appointmentId) {
        if (!enabled) {
            log.debug("Twilio disabled, skipping SMS for appointment {}", appointmentId);
            return;
        }
        Message.creator(new PhoneNumber(to), new PhoneNumber(fromNumber),
                "Reminder: You have an upcoming appointment. ID: " + appointmentId).create();
    }
}
