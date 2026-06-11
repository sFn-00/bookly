package com.bookly.domain.booking;

import com.bookly.config.TenantContext;
import com.bookly.domain.appointment.Appointment;
import com.bookly.domain.appointment.AppointmentRepository;
import com.bookly.domain.appointment.AppointmentStatus;
import com.bookly.domain.availability.Availability;
import com.bookly.domain.availability.AvailabilityService;
import com.bookly.domain.client.Client;
import com.bookly.domain.client.ClientService;
import com.bookly.domain.plan.PlanEnforcer;
import com.bookly.domain.service.ServiceService;
import com.bookly.domain.staff.StaffService;
import com.bookly.exception.ConflictException;
import com.bookly.exception.NotFoundException;
import com.bookly.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final AppointmentRepository appointmentRepository;
    private final ClientService clientService;
    private final ServiceService serviceService;
    private final StaffService staffService;
    private final AvailabilityService availabilityService;
    private final PlanEnforcer planEnforcer;

    // 8.1 calculateAvailableSlots()
    @Transactional(readOnly = true)
    public List<TimeSlot> calculateAvailableSlots(AvailableSlotsQuery query) {
        LocalDate date = query.date();

        List<Availability> availabilities = availabilityService.getByStaffAndDay(query.staffId(), date.getDayOfWeek());
        if (availabilities.isEmpty()) {
            return List.of();
        }

        List<Appointment> appointments = appointmentRepository
                .findByStaffIdAndStatusInAndStartTimeBetween(
                        query.staffId(),
                        List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED),
                        date.atStartOfDay(),
                        date.atTime(LocalTime.MAX)
                );

        int duration = serviceService.findById(query.serviceId()).getDurationMinutes();

        return availabilities.stream()
                .flatMap(window -> generateSlotsForWindow(date, window, duration, appointments).stream())
                .toList();
    }

    private List<TimeSlot> generateSlotsForWindow(
            LocalDate date, Availability window, int duration, List<Appointment> appointments) {

        List<TimeSlot> slots = new ArrayList<>();
        LocalDateTime cursor = date.atTime(window.getStartTime());
        LocalDateTime windowEnd = date.atTime(window.getEndTime());

        while (!cursor.plusMinutes(duration).isAfter(windowEnd)) {
            LocalDateTime slotStart = cursor;
            LocalDateTime slotEnd = cursor.plusMinutes(duration);

            boolean isFree = appointments.stream().noneMatch(a ->
                    TimeUtils.overlaps(slotStart, slotEnd, a.getStartTime(), a.getEndTime()));

            if (isFree) {
                slots.add(new TimeSlot(slotStart, slotEnd));
            }
            cursor = slotEnd;
        }

        return slots;
    }

    // 8.2 book()
    public Appointment book(BookRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        planEnforcer.checkMonthlyBookingLimit(tenantId);

        Client client = clientService.findOrCreate(tenantId, request.clientFirstName(),
                request.clientLastName(), request.clientEmail(), request.clientPhone());
        int duration = serviceService.findById(request.serviceId()).getDurationMinutes();

        Appointment appointment = new Appointment();
        appointment.setTenantId(tenantId);
        appointment.setClientId(client.getId());
        appointment.setStaffId(request.staffId());
        appointment.setServiceId(request.serviceId());
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.startTime().plusMinutes(duration));
        appointment.setNotes(request.notes());

        try {
            return appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
            throw new ConflictException("Slot no longer available");
        }
    }

    // 8.3 cancel()
    public void cancel(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));

        TenantContext.assertTenantMatch(appointment.getTenantId());

        if (AppointmentStatus.CANCELLED == appointment.getStatus()) {
            throw new ConflictException("Appointment is already cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
    }
}
