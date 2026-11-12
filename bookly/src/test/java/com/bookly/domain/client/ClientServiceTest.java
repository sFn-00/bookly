package com.bookly.domain.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock ClientRepository clientRepository;
    @InjectMocks ClientService clientService;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void getOrCreate_existingClient_returnsItWithoutInserting() {
        Client existing = new Client();
        when(clientRepository.findByTenantIdAndEmailIgnoreCase(tenantId, "a@x.com")).thenReturn(Optional.of(existing));

        Client result = clientService.getOrCreate(tenantId, "Ann", "Lee", "a@x.com", "123");

        assertThat(result).isSameAs(existing);
        verify(clientRepository, never()).insertIfAbsent(tenantId, "Ann", "Lee", "a@x.com", "123");
    }

    @Test
    void getOrCreate_missingClient_insertsThenReadsIt() {
        Client created = new Client();
        when(clientRepository.findByTenantIdAndEmailIgnoreCase(tenantId, "a@x.com"))
                .thenReturn(Optional.empty(), Optional.of(created));

        Client result = clientService.getOrCreate(tenantId, "Ann", "Lee", "a@x.com", "123");

        assertThat(result).isSameAs(created);
        verify(clientRepository).insertIfAbsent(tenantId, "Ann", "Lee", "a@x.com", "123");
    }
}
