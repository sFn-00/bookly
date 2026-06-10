package com.bookly.domain.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public long countActiveStaff(UUID tenantId) {
        return staffRepository.countByTenantIdAndActiveTrue(tenantId);
    }
}
