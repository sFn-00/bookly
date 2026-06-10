package com.bookly.api.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ServiceUpdateRequest(
        @NotBlank String name,
        String description,
        @Positive int durationMinutes,
        @NotNull @Positive BigDecimal price
) {}
