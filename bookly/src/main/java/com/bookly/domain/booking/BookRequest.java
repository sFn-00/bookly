package com.bookly.domain.booking;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookRequest(
        UUID staffId,
        UUID serviceId,
        LocalDateTime startTime,
        String notes,
        String clientFirstName,
        String clientLastName,
        String clientEmail,
        String clientPhone
) {}
