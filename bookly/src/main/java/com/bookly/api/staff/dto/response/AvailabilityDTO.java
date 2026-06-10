package com.bookly.api.staff.dto.response;

import com.bookly.domain.availability.Availability;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityDTO(
        UUID id,
        UUID staffId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public static AvailabilityDTO from(Availability availability) {
        return new AvailabilityDTO(
                availability.getId(),
                availability.getStaffId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime()
        );
    }
}
