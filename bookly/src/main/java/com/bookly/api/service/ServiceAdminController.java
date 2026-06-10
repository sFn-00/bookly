package com.bookly.api.service;

import com.bookly.api.service.dto.request.ServiceCreateRequest;
import com.bookly.api.service.dto.request.ServiceUpdateRequest;
import com.bookly.api.service.dto.response.ServiceDTO;
import com.bookly.domain.service.Service;
import com.bookly.domain.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.bookly.config.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
@PreAuthorize(Roles.OWNER_OR_STAFF)
public class ServiceAdminController {

    private final ServiceService serviceService;

    @GetMapping
    public ResponseEntity<List<ServiceDTO>> getAll() {
        List<ServiceDTO> services = serviceService.findAll().stream().map(this::toDto).toList();
        return ResponseEntity.ok(services);
    }

    @PostMapping
    public ResponseEntity<ServiceDTO> create(@RequestBody @Valid ServiceCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(serviceService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceDTO> update(@PathVariable UUID id, @RequestBody @Valid ServiceUpdateRequest req) {
        return ResponseEntity.ok(toDto(serviceService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        serviceService.softDelete(id);
        return ResponseEntity.noContent().build();
    }




    private ServiceDTO toDto(Service service) {
        return new ServiceDTO(service.getId(),service.getName(),service.getDescription(),
                service.getDurationMinutes(),service.getPrice());
    }
}
