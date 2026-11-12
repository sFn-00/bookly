package com.bookly.api.offering.dto.response;

import com.bookly.domain.offering.Offering;

import java.math.BigDecimal;
import java.util.UUID;

public record OfferingDTO(UUID id,
                         String name,
                         String description,
                         int durationMinutes,
                         BigDecimal price) {

    public static OfferingDTO from(Offering offering) {
        return new OfferingDTO(offering.getId(), offering.getName(), offering.getDescription(),
                offering.getDurationMinutes(), offering.getPrice());
    }
}
