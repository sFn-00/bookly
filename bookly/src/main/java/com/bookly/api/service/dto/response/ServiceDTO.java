package com.bookly.api.service.dto.response;

import com.bookly.domain.service.Service;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceDTO(UUID id,
                         String name,
                         String description,
                         int durationMinutes,
                         BigDecimal price) {

    public static ServiceDTO from(Service service) {
        return new ServiceDTO(service.getId(), service.getName(), service.getDescription(),
                service.getDurationMinutes(), service.getPrice());
    }
}
