package com.bookly.api.booking;

import com.bookly.api.booking.dto.request.BookAppointmentRequest;
import com.bookly.api.booking.dto.response.AppointmentDTO;
import com.bookly.api.offering.dto.response.OfferingDTO;
import com.bookly.domain.booking.AvailableSlotsQuery;
import com.bookly.domain.booking.BookRequest;
import com.bookly.domain.booking.BookingService;
import com.bookly.domain.booking.TimeSlot;
import com.bookly.domain.offering.OfferingService;
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
    private final OfferingService offeringService;

    @GetMapping("/offerings")
    public ResponseEntity<List<OfferingDTO>> getOfferings() {
        List<OfferingDTO> offerings = offeringService.findAll().stream()
                .map(OfferingDTO::from)
                .toList();
        return ResponseEntity.ok(offerings);
    }

    @GetMapping("/slots")
    public ResponseEntity<List<TimeSlot>> getSlots(
            @RequestParam UUID staffId,
            @RequestParam UUID offeringId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<TimeSlot> slots = bookingService.calculateAvailableSlots(
                new AvailableSlotsQuery(staffId, offeringId, date));
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentDTO> book(@Valid @RequestBody BookAppointmentRequest request) {
        AppointmentDTO dto = AppointmentDTO.from(bookingService.book(new BookRequest(
                request.staffId(),
                request.offeringId(),
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
