package com.bookly.api.staff.dto.response;

import com.bookly.domain.staff.Staff;

import java.util.UUID;

public record StaffDTO(
        UUID id,
        String firstName,
        String lastName,
        String email
) {
    public static StaffDTO from(Staff staff) {
        return new StaffDTO(staff.getId(), staff.getFirstName(), staff.getLastName(), staff.getEmail());
    }
}
