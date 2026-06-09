package com.bookly.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String companyName,
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$",
                message = "subdomain must be lowercase alphanumeric with hyphens") String subdomain,
        @NotBlank @Email String email,
        @NotBlank
        @Size(min = 8)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).*$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
        )
        String password
) {}
