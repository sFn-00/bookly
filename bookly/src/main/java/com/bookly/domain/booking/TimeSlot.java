package com.bookly.domain.booking;

import java.time.LocalDateTime;

public record TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {}
