package com.bookly.domain.booking;

import java.time.LocalDate;
import java.util.UUID;

public record AvailableSlotsQuery(UUID staffId, UUID serviceId, LocalDate date) {}
