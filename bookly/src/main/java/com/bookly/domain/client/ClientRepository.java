package com.bookly.domain.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    // ON CONFLICT DO NOTHING: a losing concurrent insert is a no-op instead of an exception,
    // so the surrounding transaction is not marked rollback-only. id and created_at use DB defaults.
    @Modifying
    @Query(value = """
            INSERT INTO clients (tenant_id, first_name, last_name, email, phone)
            VALUES (:tenantId, :firstName, :lastName, :email, :phone)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("tenantId") UUID tenantId,
                        @Param("firstName") String firstName,
                        @Param("lastName") String lastName,
                        @Param("email") String email,
                        @Param("phone") String phone);
}
