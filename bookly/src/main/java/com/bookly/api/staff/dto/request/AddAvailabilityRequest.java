package com.bookly.api.staff.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record AddAvailabilityRequest(
        @NotNull UUID staffId,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {
    @AssertTrue(message = "endTime must be after startTime")
    public boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) return true;
        return endTime.isAfter(startTime);
    }
}
