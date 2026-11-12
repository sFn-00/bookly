package com.bookly.api.booking.dto.response;

import com.bookly.api.offering.dto.response.OfferingSummaryDTO;
import com.bookly.api.staff.dto.response.StaffSummaryDTO;
import com.bookly.domain.appointment.Appointment;
import com.bookly.domain.appointment.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentDTO(
        UUID id,
        StaffSummaryDTO staff,
        OfferingSummaryDTO offering,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String notes
) {
    public static AppointmentDTO from(Appointment appointment) {
        return new AppointmentDTO(
                appointment.getId(),
                StaffSummaryDTO.from(appointment.getStaff()),
                OfferingSummaryDTO.from(appointment.getOffering()),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getNotes()
        );
    }
}
