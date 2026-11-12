package com.bookly.api.offering;

import com.bookly.api.offering.dto.request.OfferingCreateRequest;
import com.bookly.api.offering.dto.request.OfferingUpdateRequest;
import com.bookly.config.SecurityConfig;
import com.bookly.config.TenantFilter;
import com.bookly.domain.offering.Offering;
import com.bookly.domain.offering.OfferingService;
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

@WebMvcTest(OfferingAdminController.class)
@Import(SecurityConfig.class)
class OfferingAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean OfferingService offeringService;
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
    void getAll_returnsListOfOfferings() throws Exception {
        Offering s = buildOffering("Haircut");
        when(offeringService.findAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/admin/offerings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Haircut"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void create_validRequest_returns201() throws Exception {
        OfferingCreateRequest req = new OfferingCreateRequest("Haircut", "desc", 30, BigDecimal.valueOf(50));
        when(offeringService.create(any())).thenReturn(buildOffering("Haircut"));

        mockMvc.perform(post("/api/admin/offerings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Haircut"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void create_invalidRequest_returns400() throws Exception {
        OfferingCreateRequest req = new OfferingCreateRequest("", null, -1, null);

        mockMvc.perform(post("/api/admin/offerings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void update_validRequest_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        OfferingUpdateRequest req = new OfferingUpdateRequest("New Name", "desc", 60, BigDecimal.valueOf(100));
        when(offeringService.update(eq(id), any())).thenReturn(buildOffering("New Name"));

        mockMvc.perform(put("/api/admin/offerings/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void delete_existingOffering_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/offerings/" + id))
                .andExpect(status().isNoContent());

        verify(offeringService).softDelete(id);
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void delete_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("Offering not found")).when(offeringService).softDelete(id);

        mockMvc.perform(delete("/api/admin/offerings/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void delete_hasUpcomingAppointments_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ConflictException("Offering has upcoming appointments")).when(offeringService).softDelete(id);

        mockMvc.perform(delete("/api/admin/offerings/" + id))
                .andExpect(status().isConflict());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/offerings"))
                .andExpect(status().isForbidden());
    }

    private Offering buildOffering(String name) {
        Offering s = new Offering();
        s.setId(UUID.randomUUID());
        s.setTenantId(UUID.randomUUID());
        s.setName(name);
        s.setDurationMinutes(30);
        s.setPrice(BigDecimal.valueOf(50));
        s.setActive(true);
        return s;
    }
}
