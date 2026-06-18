package com.bookly.domain.staff;

import com.bookly.api.staff.dto.request.StaffCreateRequest;
import com.bookly.config.TenantContext;
import com.bookly.domain.plan.PlanEnforcer;
import com.bookly.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock StaffRepository staffRepository;
    @Mock PlanEnforcer planEnforcer;
    @InjectMocks StaffService staffService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenant(tenantId.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_savesStaffWithTenantId() {
        StaffCreateRequest req = new StaffCreateRequest("Alice", "Smith", "alice@example.com");
        Staff saved = buildStaff("Alice", "Smith");
        when(staffRepository.save(any())).thenReturn(saved);

        Staff result = staffService.create(req);

        ArgumentCaptor<Staff> captor = ArgumentCaptor.forClass(Staff.class);
        verify(staffRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().getFirstName()).isEqualTo("Alice");
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void create_checksPlanLimit() {
        StaffCreateRequest req = new StaffCreateRequest("Alice", "Smith", "alice@example.com");
        when(staffRepository.save(any())).thenReturn(buildStaff("Alice", "Smith"));

        staffService.create(req);

        verify(planEnforcer).checkStaffLimit(eq(tenantId), anyLong());
    }

    @Test
    void findAll_returnsActiveStaffForTenant() {
        List<Staff> staff = List.of(buildStaff("Alice", "Smith"));
        when(staffRepository.findAllByTenantIdAndActiveTrue(tenantId)).thenReturn(staff);

        List<Staff> result = staffService.findAll();

        assertThat(result).isEqualTo(staff);
    }

    @Test
    void findById_found_returnsStaff() {
        UUID id = UUID.randomUUID();
        Staff staff = buildStaff("Alice", "Smith");
        when(staffRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(staff));

        Staff result = staffService.findById(id);

        assertThat(result).isEqualTo(staff);
    }

    @Test
    void findById_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(staffRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void softDelete_setsActiveToFalse() {
        UUID id = UUID.randomUUID();
        Staff staff = buildStaff("Alice", "Smith");
        when(staffRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(staff));

        staffService.softDelete(id);

        assertThat(staff.isActive()).isFalse();
    }

    @Test
    void countActiveStaff_delegatesToRepository() {
        when(staffRepository.countByTenantIdAndActiveTrue(tenantId)).thenReturn(3L);

        assertThat(staffService.countActiveStaff(tenantId)).isEqualTo(3L);
    }

    private Staff buildStaff(String firstName, String lastName) {
        Staff s = new Staff();
        s.setId(UUID.randomUUID());
        s.setTenantId(tenantId);
        s.setFirstName(firstName);
        s.setLastName(lastName);
        s.setEmail(firstName.toLowerCase() + "@example.com");
        s.setActive(true);
        return s;
    }
}
