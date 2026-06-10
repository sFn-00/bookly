package com.bookly.api.service;

import com.bookly.api.service.dto.request.ServiceCreateRequest;
import com.bookly.api.service.dto.request.ServiceUpdateRequest;
import com.bookly.config.SecurityConfig;
import com.bookly.config.TenantFilter;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceAdminController.class)
@Import(SecurityConfig.class)
class ServiceAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
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
    @WithMockUser(roles = "OWNER")
    void getAll_returnsListOfServices() throws Exception {
        Service s = buildService("Haircut");
        when(serviceService.findAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/admin/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Haircut"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void create_validRequest_returns201() throws Exception {
        ServiceCreateRequest req = new ServiceCreateRequest("Haircut", "desc", 30, BigDecimal.valueOf(50));
        when(serviceService.create(any())).thenReturn(buildService("Haircut"));

        mockMvc.perform(post("/api/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Haircut"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void create_invalidRequest_returns400() throws Exception {
        ServiceCreateRequest req = new ServiceCreateRequest("", null, -1, null);

        mockMvc.perform(post("/api/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void update_validRequest_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        ServiceUpdateRequest req = new ServiceUpdateRequest("New Name", "desc", 60, BigDecimal.valueOf(100));
        when(serviceService.update(eq(id), any())).thenReturn(buildService("New Name"));

        mockMvc.perform(put("/api/admin/services/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void delete_existingService_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/services/" + id))
                .andExpect(status().isNoContent());

        verify(serviceService).softDelete(id);
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void delete_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("Service not found")).when(serviceService).softDelete(id);

        mockMvc.perform(delete("/api/admin/services/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void delete_hasUpcomingAppointments_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ConflictException("Service has upcoming appointments")).when(serviceService).softDelete(id);

        mockMvc.perform(delete("/api/admin/services/" + id))
                .andExpect(status().isConflict());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/services"))
                .andExpect(status().isForbidden());
    }

    private Service buildService(String name) {
        Service s = new Service();
        s.setId(UUID.randomUUID());
        s.setTenantId(UUID.randomUUID());
        s.setName(name);
        s.setDurationMinutes(30);
        s.setPrice(BigDecimal.valueOf(50));
        s.setActive(true);
        return s;
    }
}
