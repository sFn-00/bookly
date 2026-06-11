package com.bookly.api.appointment;

import com.bookly.api.booking.dto.response.AppointmentDTO;
import com.bookly.config.Roles;
import com.bookly.domain.appointment.AppointmentFilter;
import com.bookly.domain.appointment.AppointmentService;
import com.bookly.domain.appointment.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/appointments")
@PreAuthorize(Roles.OWNER_OR_STAFF)
@RequiredArgsConstructor
public class AppointmentAdminController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) AppointmentStatus status
    ) {
        List<AppointmentDTO> appointments = appointmentService.findAll(new AppointmentFilter(from, to, status))
                .stream()
                .map(AppointmentDTO::from)
                .toList();
        return ResponseEntity.ok(appointments);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<AppointmentDTO> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(AppointmentDTO.from(appointmentService.confirm(id)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        appointmentService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
