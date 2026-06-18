package com.bookly.domain.staff;

import com.bookly.api.staff.dto.request.StaffCreateRequest;
import com.bookly.config.TenantContext;
import com.bookly.domain.plan.PlanEnforcer;
import com.bookly.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffService {

    private final StaffRepository staffRepository;
    private final PlanEnforcer planEnforcer;

    public Staff create(StaffCreateRequest req){
       UUID tenantUUID = TenantContext.getTenantId();
       long currentCount = staffRepository.countByTenantIdAndActiveTrue(tenantUUID);
       planEnforcer.checkStaffLimit(tenantUUID, currentCount);

       Staff staff = new Staff();
       staff.setTenantId(tenantUUID);
       staff.setFirstName(req.firstName());
       staff.setLastName(req.lastName());
       staff.setEmail(req.email());
       return staffRepository.save(staff);
    }


    @Transactional(readOnly = true)
    public List<Staff> findAll() {
        return staffRepository.findAllByTenantIdAndActiveTrue(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public Staff findById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return staffRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));
    }

    public void softDelete(UUID id) {
        Staff staff = findById(id);
        staff.setActive(false);
    }

    @Transactional(readOnly = true)
    public long countActiveStaff(UUID tenantId) {
        return staffRepository.countByTenantIdAndActiveTrue(tenantId);
    }
}
