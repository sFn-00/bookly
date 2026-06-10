package com.bookly.api.staff;

import com.bookly.api.staff.dto.request.StaffCreateRequest;
import com.bookly.config.SecurityConfig;
import com.bookly.config.TenantFilter;
import com.bookly.domain.staff.Staff;
import com.bookly.domain.staff.StaffService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StaffAdminController.class)
@Import(SecurityConfig.class)
class StaffAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean StaffService staffService;
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
    void getAll_returnsStaffList() throws Exception {
        when(staffService.findAll()).thenReturn(List.of(buildStaff("Alice", "Smith")));

        mockMvc.perform(get("/api/admin/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Alice"))
                .andExpect(jsonPath("$[0].lastName").value("Smith"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getAll_asStaff_returns200() throws Exception {
        when(staffService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/staff"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void create_validRequest_returns201() throws Exception {
        StaffCreateRequest req = new StaffCreateRequest("Alice", "Smith", "alice@example.com");
        when(staffService.create(any())).thenReturn(buildStaff("Alice", "Smith"));

        mockMvc.perform(post("/api/admin/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void create_invalidRequest_returns400() throws Exception {
        StaffCreateRequest req = new StaffCreateRequest("", "", "not-an-email");

        mockMvc.perform(post("/api/admin/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void create_asStaff_returns403() throws Exception {
        StaffCreateRequest req = new StaffCreateRequest("Alice", "Smith", "alice@example.com");

        mockMvc.perform(post("/api/admin/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void delete_existingStaff_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/staff/" + id))
                .andExpect(status().isNoContent());

        verify(staffService).softDelete(id);
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void delete_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("Staff not found")).when(staffService).softDelete(id);

        mockMvc.perform(delete("/api/admin/staff/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/staff"))
                .andExpect(status().isForbidden());
    }

    private Staff buildStaff(String firstName, String lastName) {
        Staff s = new Staff();
        s.setId(UUID.randomUUID());
        s.setTenantId(UUID.randomUUID());
        s.setFirstName(firstName);
        s.setLastName(lastName);
        s.setEmail(firstName.toLowerCase() + "@example.com");
        s.setActive(true);
        return s;
    }
}
