package com.bookly.api.staff;

import com.bookly.api.staff.dto.request.AddAvailabilityRequest;
import com.bookly.config.SecurityConfig;
import com.bookly.config.TenantFilter;
import com.bookly.domain.availability.Availability;
import com.bookly.domain.availability.AvailabilityService;
import com.bookly.exception.ConflictException;
import com.bookly.infrastructure.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AvailabilityAdminController.class)
@Import(SecurityConfig.class)
class AvailabilityAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AvailabilityService availabilityService;
    @MockitoBean JwtService jwtService;
    @MockitoBean TenantFilter tenantFilter;

    private final UUID staffId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            inv.getArgument(2, FilterChain.class).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(tenantFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void getByStaff_returnsAvailabilityList() throws Exception {
        when(availabilityService.getByStaff(staffId))
                .thenReturn(List.of(buildAvailability(staffId, DayOfWeek.MONDAY)));

        mockMvc.perform(get("/api/admin/staff/" + staffId + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].staffId").value(staffId.toString()));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getByStaff_asStaff_returns200() throws Exception {
        when(availabilityService.getByStaff(staffId)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/staff/" + staffId + "/availability"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void add_validRequest_returns201() throws Exception {
        AddAvailabilityRequest req = new AddAvailabilityRequest(
                staffId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        when(availabilityService.addAvailability(any()))
                .thenReturn(buildAvailability(staffId, DayOfWeek.MONDAY));

        mockMvc.perform(post("/api/admin/staff/" + staffId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void add_endTimeBeforeStartTime_returns400() throws Exception {
        AddAvailabilityRequest req = new AddAvailabilityRequest(
                staffId, DayOfWeek.MONDAY, LocalTime.of(17, 0), LocalTime.of(9, 0));

        mockMvc.perform(post("/api/admin/staff/" + staffId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void add_overlappingSlot_returns409() throws Exception {
        AddAvailabilityRequest req = new AddAvailabilityRequest(
                staffId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        when(availabilityService.addAvailability(any()))
                .thenThrow(new ConflictException("Availability slot overlaps with existing schedule"));

        mockMvc.perform(post("/api/admin/staff/" + staffId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void add_asStaff_returns403() throws Exception {
        AddAvailabilityRequest req = new AddAvailabilityRequest(
                staffId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));

        mockMvc.perform(post("/api/admin/staff/" + staffId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByStaff_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/staff/" + staffId + "/availability"))
                .andExpect(status().isForbidden());
    }

    private Availability buildAvailability(UUID staffId, DayOfWeek day) {
        Availability a = new Availability();
        a.setId(UUID.randomUUID());
        a.setTenantId(UUID.randomUUID());
        a.setStaffId(staffId);
        a.setDayOfWeek(day);
        a.setStartTime(LocalTime.of(9, 0));
        a.setEndTime(LocalTime.of(17, 0));
        return a;
    }
}
