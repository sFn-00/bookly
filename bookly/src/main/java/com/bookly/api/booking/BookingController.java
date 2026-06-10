package com.bookly.api.booking;

import com.bookly.api.booking.dto.request.BookAppointmentRequest;
import com.bookly.api.booking.dto.response.AppointmentDTO;
import com.bookly.api.service.dto.response.ServiceDTO;
import com.bookly.domain.booking.AvailableSlotsQuery;
import com.bookly.domain.booking.BookRequest;
import com.bookly.domain.booking.BookingService;
import com.bookly.domain.booking.TimeSlot;
import com.bookly.domain.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final ServiceService serviceService;

    @GetMapping("/services")
    public ResponseEntity<List<ServiceDTO>> getServices() {
        List<ServiceDTO> services = serviceService.findAll().stream()
                .map(ServiceDTO::from)
                .toList();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/slots")
    public ResponseEntity<List<TimeSlot>> getSlots(
            @RequestParam UUID staffId,
            @RequestParam UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<TimeSlot> slots = bookingService.calculateAvailableSlots(
                new AvailableSlotsQuery(staffId, serviceId, date));
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentDTO> book(@Valid @RequestBody BookAppointmentRequest request) {
        AppointmentDTO dto = AppointmentDTO.from(bookingService.book(new BookRequest(
                request.staffId(),
                request.serviceId(),
                request.startTime(),
                request.notes(),
                request.clientFirstName(),
                request.clientLastName(),
                request.clientEmail(),
                request.clientPhone()
        )));
        return ResponseEntity.created(URI.create("/api/booking/appointments/" + dto.id())).body(dto);
    }

    @PutMapping("/appointments/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        bookingService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
