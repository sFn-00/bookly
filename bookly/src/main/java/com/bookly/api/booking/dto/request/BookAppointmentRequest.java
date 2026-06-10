package com.bookly.api.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookAppointmentRequest(
        @NotNull UUID staffId,
        @NotNull UUID serviceId,
        @NotNull LocalDateTime startTime,
        @NotBlank String clientFirstName,
        @NotBlank String clientLastName,
        @NotBlank @Email String clientEmail,
        String clientPhone,
        String notes
) {}
