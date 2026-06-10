package com.bookly.api.service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceDTO(UUID id,
                         String name,
                         String description,
                         int durationMinutes,
                         BigDecimal price)
{}
