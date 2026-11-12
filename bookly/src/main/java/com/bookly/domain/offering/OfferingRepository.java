package com.bookly.domain.offering;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferingRepository extends JpaRepository<Offering, UUID> {

    List<Offering> findAllByTenantIdAndActiveTrue(UUID tenantId);

    Optional<Offering> findByIdAndTenantIdAndActiveTrue(UUID id, UUID tenantId);
}
