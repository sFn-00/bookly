package com.bookly.domain.plan;

import com.bookly.domain.appointment.AppointmentService;
import com.bookly.domain.tenant.Plan;
import com.bookly.domain.tenant.TenantService;
import com.bookly.exception.PlanLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanEnforcer {

    private final TenantService tenantService;
    private final AppointmentService appointmentService;

    public void checkStaffLimit(UUID tenantId, long currentCount) {
        Plan plan = tenantService.findById(tenantId).getPlan();
        if (plan.hasUnlimitedStaff()) return;

        if (currentCount >= plan.getMaxStaff()) {
            throw new PlanLimitException(
                    "Staff limit reached for plan " + plan + " (max " + plan.getMaxStaff() + ")"
            );
        }
    }

    public void checkMonthlyBookingLimit(UUID tenantId) {
        Plan plan = tenantService.findById(tenantId).getPlan();
        if (plan.hasUnlimitedBookings()) return;

        long current = appointmentService.countMonthlyBookings(tenantId);
        if (current >= plan.getMaxMonthlyBookings()) {
            throw new PlanLimitException(
                    "Monthly booking limit reached for plan " + plan + " (max " + plan.getMaxMonthlyBookings() + ")"
            );
        }
    }
}
