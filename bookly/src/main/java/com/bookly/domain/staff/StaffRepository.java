package com.bookly.domain.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    long countByTenantIdAndActiveTrue(UUID tenantId);

    List<Staff> findAllByTenantIdAndActiveTrue(UUID tenantId);

    Optional<Staff> findByIdAndTenantId(UUID id, UUID tenantId);
}
