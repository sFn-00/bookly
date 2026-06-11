package com.bookly.domain.appointment;

import java.time.LocalDate;

public record AppointmentFilter(LocalDate from, LocalDate to, AppointmentStatus status) {}
