package com.bookly.api.staff.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StaffCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email
)
{}