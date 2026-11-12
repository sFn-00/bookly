package com.bookly.api.offering.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OfferingCreateRequest(
        @NotBlank String name,
        String description,
        @Positive int durationMinutes,
        @NotNull @Positive BigDecimal price
) {}
