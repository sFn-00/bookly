package com.bookly.domain.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Optional<Client> findById(UUID id) {
        return clientRepository.findById(id);
    }

    /**
     * Returns the tenant's client with this email, creating it when missing.
     * Not a pure query (it may write), hence "get" instead of "find".
     * Name and phone of an existing client are intentionally NOT overwritten: the booking
     * endpoint is public, so anyone knowing an email could otherwise change that client's data.
     */
    public Client getOrCreate(UUID tenantId, String firstName, String lastName, String email, String phone) {
        Optional<Client> existing = clientRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email);
        if (existing.isPresent()) {
            return existing.get();
        }

        clientRepository.insertIfAbsent(tenantId, firstName, lastName, email, phone);

        // Re-read: either our row or the one a concurrent request inserted first.
        return clientRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
                .orElseThrow(() -> new IllegalStateException("Client missing after insert"));
    }
}
