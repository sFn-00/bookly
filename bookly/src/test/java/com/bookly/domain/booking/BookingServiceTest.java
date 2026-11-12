package com.bookly.domain.booking;

import com.bookly.config.TenantContext;
import com.bookly.domain.appointment.Appointment;
import com.bookly.domain.appointment.AppointmentRepository;
import com.bookly.domain.availability.Availability;
import com.bookly.domain.availability.AvailabilityService;
import com.bookly.domain.client.Client;
import com.bookly.domain.client.ClientService;
import com.bookly.domain.appointment.AppointmentStatus;
import com.bookly.domain.plan.PlanEnforcer;
import com.bookly.domain.offering.Offering;
import com.bookly.domain.offering.OfferingService;
import com.bookly.domain.staff.Staff;
import com.bookly.domain.staff.StaffService;
import com.bookly.exception.ConflictException;
import com.bookly.exception.NotFoundException;
import com.bookly.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock ClientService clientService;
    @Mock OfferingService offeringService;
    @Mock StaffService staffService;
    @Mock AvailabilityService availabilityService;
    @Mock PlanEnforcer planEnforcer;
    @InjectMocks BookingService bookingService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final Staff staff = buildStaff();
    private final UUID offeringId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenant(tenantId.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // --- calculateAvailableSlots ---

    @Test
    void calculateAvailableSlots_noAvailability_returnsEmpty() {
        LocalDate monday = LocalDate.of(2025, 1, 6);
        when(availabilityService.getByStaffAndDay(staffId, DayOfWeek.MONDAY)).thenReturn(List.of());

        List<TimeSlot> slots = bookingService.calculateAvailableSlots(
                new AvailableSlotsQuery(staffId, offeringId, monday));

        assertThat(slots).isEmpty();
    }

    @Test
    void calculateAvailableSlots_noAppointments_returnsAllSlots() {
        LocalDate monday = LocalDate.of(2025, 1, 6);
        Availability window = buildAvailability(LocalTime.of(9, 0), LocalTime.of(10, 0));
        Offering offering = buildOffering(30);

        when(availabilityService.getByStaffAndDay(staffId, DayOfWeek.MONDAY)).thenReturn(List.of(window));
        when(appointmentRepository.findByStaffIdAndStatusInAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(offeringService.findById(offeringId)).thenReturn(offering);

        List<TimeSlot> slots = bookingService.calculateAvailableSlots(
                new AvailableSlotsQuery(staffId, offeringId, monday));

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).startTime()).isEqualTo(monday.atTime(9, 0));
        assertThat(slots.get(1).startTime()).isEqualTo(monday.atTime(9, 30));
    }

    @Test
    void calculateAvailableSlots_withAppointment_blocksOverlappingSlot() {
        LocalDate monday = LocalDate.of(2025, 1, 6);
        Availability window = buildAvailability(LocalTime.of(9, 0), LocalTime.of(10, 0));
        Offering offering = buildOffering(30);

        Appointment existing = new Appointment();
        existing.setStartTime(monday.atTime(9, 0));
        existing.setEndTime(monday.atTime(9, 30));

        when(availabilityService.getByStaffAndDay(staffId, DayOfWeek.MONDAY)).thenReturn(List.of(window));
        when(appointmentRepository.findByStaffIdAndStatusInAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of(existing));
        when(offeringService.findById(offeringId)).thenReturn(offering);

        List<TimeSlot> slots = bookingService.calculateAvailableSlots(
                new AvailableSlotsQuery(staffId, offeringId, monday));

        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).startTime()).isEqualTo(monday.atTime(9, 30));
    }

    // --- book ---

    @Test
    void book_happyPath_savesAndReturnsAppointment() {
        BookRequest request = buildBookRequest(LocalDateTime.of(2025, 1, 6, 9, 0));
        Client client = new Client();
        client.setId(UUID.randomUUID());
        Appointment saved = new Appointment();
        saved.setId(UUID.randomUUID());

        when(clientService.getOrCreate(any(), any(), any(), any(), any())).thenReturn(client);
        when(offeringService.findById(offeringId)).thenReturn(buildOffering(30));
        when(staffService.findById(staffId)).thenReturn(staff);
        when(appointmentRepository.save(any())).thenReturn(saved);

        Appointment result = bookingService.book(request);

        assertThat(result).isEqualTo(saved);
        verify(planEnforcer).checkMonthlyBookingLimit(tenantId);
        verify(appointmentRepository).save(any());
    }

    @Test
    void book_slotConflict_throwsConflictException() {
        BookRequest request = buildBookRequest(LocalDateTime.of(2025, 1, 6, 9, 0));
        Client client = new Client();
        client.setId(UUID.randomUUID());

        when(clientService.getOrCreate(any(), any(), any(), any(), any())).thenReturn(client);
        when(offeringService.findById(offeringId)).thenReturn(buildOffering(30));
        when(staffService.findById(staffId)).thenReturn(staff);
        when(appointmentRepository.save(any())).thenThrow(new DataIntegrityViolationException("conflict"));

        assertThatThrownBy(() -> bookingService.book(request))
                .isInstanceOf(ConflictException.class);
    }

    // --- cancel ---

    @Test
    void cancel_happyPath_setsStatusCancelled() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setTenantId(tenantId);
        appointment.setStatus(AppointmentStatus.PENDING);

        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        bookingService.cancel(id);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancel_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancel(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancel_tenantMismatch_throwsUnauthorizedException() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setTenantId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.PENDING);

        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> bookingService.cancel(id))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void cancel_alreadyCancelled_throwsConflictException() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setTenantId(tenantId);
        appointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> bookingService.cancel(id))
                .isInstanceOf(ConflictException.class);
    }

    private Availability buildAvailability(LocalTime start, LocalTime end) {
        Availability a = new Availability();
        a.setStaff(staff);
        a.setDayOfWeek(DayOfWeek.MONDAY);
        a.setStartTime(start);
        a.setEndTime(end);
        return a;
    }

    private Offering buildOffering(int durationMinutes) {
        Offering s = new Offering();
        s.setId(offeringId);
        s.setDurationMinutes(durationMinutes);
        return s;
    }

    private BookRequest buildBookRequest(LocalDateTime startTime) {
        return new BookRequest(staffId, offeringId, startTime, null,
                "Jan", "Kowalski", "jan@example.com", null);
    }

    private Staff buildStaff() {
        Staff s = new Staff();
        s.setId(staffId);
        return s;
    }
}
