package com.bookly.domain.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    List<Service> findAllByTenantIdAndActiveTrue(UUID tenantId);

    Optional<Service> findByIdAndTenantIdAndActiveTrue(UUID id, UUID tenantId);
}
