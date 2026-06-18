package com.bookly.domain.plan;

import com.bookly.domain.appointment.AppointmentService;
import com.bookly.domain.tenant.Plan;
import com.bookly.domain.tenant.Tenant;
import com.bookly.domain.tenant.TenantService;
import com.bookly.exception.PlanLimitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanEnforcerTest {

    @Mock TenantService tenantService;
    @Mock AppointmentService appointmentService;
    @InjectMocks PlanEnforcer planEnforcer;

    @Test
    void checkStaffLimit_freePlanAtLimit_throwsPlanLimitException() {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.findById(tenantId)).thenReturn(tenantWithPlan(Plan.FREE));

        assertThatThrownBy(() -> planEnforcer.checkStaffLimit(tenantId, 1L))
                .isInstanceOf(PlanLimitException.class);
    }

    @Test
    void checkStaffLimit_freePlanBelowLimit_doesNotThrow() {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.findById(tenantId)).thenReturn(tenantWithPlan(Plan.FREE));

        assertThatCode(() -> planEnforcer.checkStaffLimit(tenantId, 0L)).doesNotThrowAnyException();
    }

    @Test
    void checkStaffLimit_proPlanAtLimit_throwsPlanLimitException() {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.findById(tenantId)).thenReturn(tenantWithPlan(Plan.PRO));

        assertThatThrownBy(() -> planEnforcer.checkStaffLimit(tenantId, 5L))
                .isInstanceOf(PlanLimitException.class);
    }

    @Test
    void checkStaffLimit_enterprisePlan_neverThrows() {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.findById(tenantId)).thenReturn(tenantWithPlan(Plan.ENTERPRISE));

        assertThatCode(() -> planEnforcer.checkStaffLimit(tenantId, 999L)).doesNotThrowAnyException();
    }

    @Test
    void checkMonthlyBookingLimit_freePlanAtLimit_throwsPlanLimitException() {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.findById(tenantId)).thenReturn(tenantWithPlan(Plan.FREE));
        when(appointmentService.countMonthlyBookings(tenantId)).thenReturn(50L);

        assertThatThrownBy(() -> planEnforcer.checkMonthlyBookingLimit(tenantId))
                .isInstanceOf(PlanLimitException.class);
    }

    @Test
    void checkMonthlyBookingLimit_freePlanBelowLimit_doesNotThrow() {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.findById(tenantId)).thenReturn(tenantWithPlan(Plan.FREE));
        when(appointmentService.countMonthlyBookings(tenantId)).thenReturn(49L);

        assertThatCode(() -> planEnforcer.checkMonthlyBookingLimit(tenantId)).doesNotThrowAnyException();
    }

    @Test
    void checkMonthlyBookingLimit_proPlan_neverThrows() {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.findById(tenantId)).thenReturn(tenantWithPlan(Plan.PRO));

        assertThatCode(() -> planEnforcer.checkMonthlyBookingLimit(tenantId)).doesNotThrowAnyException();
    }

    private Tenant tenantWithPlan(Plan plan) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setPlan(plan);
        return tenant;
    }
}
