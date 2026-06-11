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

    public Client findOrCreate(UUID tenantId, String firstName, String lastName, String email, String phone) {
        return clientRepository.findByTenantIdAndEmail(tenantId, email)
                .orElseGet(() -> {
                    Client c = new Client();
                    c.setTenantId(tenantId);
                    c.setFirstName(firstName);
                    c.setLastName(lastName);
                    c.setEmail(email);
                    c.setPhone(phone);
                    return clientRepository.save(c);
                });
    }
}
