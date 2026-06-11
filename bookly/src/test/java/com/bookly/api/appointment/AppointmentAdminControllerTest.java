package com.bookly.api.appointment;

import com.bookly.config.SecurityConfig;
import com.bookly.config.TenantFilter;
import com.bookly.domain.appointment.Appointment;
import com.bookly.domain.appointment.AppointmentService;
import com.bookly.domain.appointment.AppointmentStatus;
import com.bookly.exception.ConflictException;
import com.bookly.exception.NotFoundException;
import com.bookly.infrastructure.jwt.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentAdminController.class)
@Import(SecurityConfig.class)
class AppointmentAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AppointmentService appointmentService;
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
    @WithMockUser(roles = "OWNER")
    void getAll_returnsAppointments() throws Exception {
        when(appointmentService.findAll(any())).thenReturn(List.of(buildAppointment(AppointmentStatus.PENDING)));

        mockMvc.perform(get("/api/admin/appointments")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getAll_asStaff_returns200() throws Exception {
        when(appointmentService.findAll(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/appointments")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void confirm_existingAppointment_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(AppointmentStatus.CONFIRMED);
        when(appointmentService.confirm(id)).thenReturn(appointment);

        mockMvc.perform(put("/api/admin/appointments/" + id + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void confirm_asStaff_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(appointmentService.confirm(id)).thenReturn(buildAppointment(AppointmentStatus.CONFIRMED));

        mockMvc.perform(put("/api/admin/appointments/" + id + "/confirm"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void confirm_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(appointmentService.confirm(id)).thenThrow(new NotFoundException("Appointment not found"));

        mockMvc.perform(put("/api/admin/appointments/" + id + "/confirm"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void cancel_existingAppointment_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/admin/appointments/" + id + "/cancel"))
                .andExpect(status().isNoContent());

        verify(appointmentService).cancel(id);
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void cancel_alreadyCancelled_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ConflictException("Already cancelled")).when(appointmentService).cancel(id);

        mockMvc.perform(put("/api/admin/appointments/" + id + "/cancel"))
                .andExpect(status().isConflict());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/appointments")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31"))
                .andExpect(status().isForbidden());
    }

    private Appointment buildAppointment(AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setId(UUID.randomUUID());
        a.setTenantId(UUID.randomUUID());
        a.setStaffId(UUID.randomUUID());
        a.setServiceId(UUID.randomUUID());
        a.setStartTime(LocalDateTime.of(2025, 1, 6, 10, 0));
        a.setEndTime(LocalDateTime.of(2025, 1, 6, 10, 30));
        a.setStatus(status);
        return a;
    }
}
