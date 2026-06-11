package com.bookly.api.booking;

import com.bookly.api.booking.dto.request.BookAppointmentRequest;
import com.bookly.config.SecurityConfig;
import com.bookly.config.TenantFilter;
import com.bookly.domain.appointment.Appointment;
import com.bookly.domain.booking.AvailableSlotsQuery;
import com.bookly.domain.booking.BookingService;
import com.bookly.domain.appointment.AppointmentStatus;
import com.bookly.domain.booking.TimeSlot;
import com.bookly.domain.service.Service;
import com.bookly.domain.service.ServiceService;
import com.bookly.exception.ConflictException;
import com.bookly.exception.NotFoundException;
import com.bookly.infrastructure.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean BookingService bookingService;
    @MockitoBean ServiceService serviceService;
    @MockitoBean JwtService jwtService;
    @MockitoBean TenantFilter tenantFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            inv.getArgument(2, FilterChain.class).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(tenantFilter).doFilter(any(), any(), any());
    }

    @Test
    void getServices_noAuth_returns200() throws Exception {
        Service s = buildService("Haircut");
        when(serviceService.findAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/booking/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Haircut"));
    }

    @Test
    void getSlots_validParams_returns200() throws Exception {
        UUID staffId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2025, 1, 6);
        TimeSlot slot = new TimeSlot(date.atTime(9, 0), date.atTime(9, 30));

        when(bookingService.calculateAvailableSlots(any(AvailableSlotsQuery.class)))
                .thenReturn(List.of(slot));

        mockMvc.perform(get("/api/booking/slots")
                        .param("staffId", staffId.toString())
                        .param("serviceId", serviceId.toString())
                        .param("date", "2025-01-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startTime").exists());
    }

    @Test
    void book_validRequest_returns201() throws Exception {
        BookAppointmentRequest req = new BookAppointmentRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.of(2025, 1, 6, 9, 0),
                "Jan", "Kowalski", "jan@example.com", null, null);

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setStaffId(req.staffId());
        appointment.setServiceId(req.serviceId());
        appointment.setStartTime(req.startTime());
        appointment.setEndTime(req.startTime().plusMinutes(30));
        appointment.setStatus(AppointmentStatus.PENDING);

        when(bookingService.book(any())).thenReturn(appointment);

        mockMvc.perform(post("/api/booking/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")); // enum serializes to its name
    }

    @Test
    void book_invalidRequest_returns400() throws Exception {
        BookAppointmentRequest req = new BookAppointmentRequest(
                null, null, null, "", "", "not-email", null, null);

        mockMvc.perform(post("/api/booking/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void book_slotConflict_returns409() throws Exception {
        BookAppointmentRequest req = new BookAppointmentRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.of(2025, 1, 6, 9, 0),
                "Jan", "Kowalski", "jan@example.com", null, null);

        when(bookingService.book(any())).thenThrow(new ConflictException("Slot no longer available"));

        mockMvc.perform(post("/api/booking/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void cancel_existingAppointment_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/booking/appointments/" + id + "/cancel"))
                .andExpect(status().isNoContent());

        verify(bookingService).cancel(id);
    }

    @Test
    void cancel_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("Appointment not found")).when(bookingService).cancel(id);

        mockMvc.perform(put("/api/booking/appointments/" + id + "/cancel"))
                .andExpect(status().isNotFound());
    }

    private Service buildService(String name) {
        Service s = new Service();
        s.setId(UUID.randomUUID());
        s.setName(name);
        s.setDurationMinutes(30);
        s.setPrice(BigDecimal.valueOf(50));
        return s;
    }
}
