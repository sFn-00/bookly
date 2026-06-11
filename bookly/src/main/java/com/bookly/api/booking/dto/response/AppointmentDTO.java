package com.bookly.api.booking.dto.response;

import com.bookly.domain.appointment.Appointment;
import com.bookly.domain.appointment.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentDTO(
        UUID id,
        UUID staffId,
        UUID serviceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String notes
) {
    public static AppointmentDTO from(Appointment appointment) {
        return new AppointmentDTO(
                appointment.getId(),
                appointment.getStaffId(),
                appointment.getServiceId(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getNotes()
        );
    }
}
