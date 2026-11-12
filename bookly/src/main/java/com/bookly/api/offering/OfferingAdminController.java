package com.bookly.api.offering;

import com.bookly.api.offering.dto.request.OfferingCreateRequest;
import com.bookly.api.offering.dto.request.OfferingUpdateRequest;
import com.bookly.api.offering.dto.response.OfferingDTO;
import com.bookly.domain.offering.OfferingService;
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
@RequestMapping("/api/admin/offerings")
@RequiredArgsConstructor
@PreAuthorize(Roles.OWNER_OR_STAFF)
public class OfferingAdminController {

    private final OfferingService offeringService;

    @GetMapping
    public ResponseEntity<List<OfferingDTO>> getAll() {
        List<OfferingDTO> offerings = offeringService.findAll().stream().map(OfferingDTO::from).toList();
        return ResponseEntity.ok(offerings);
    }

    @PostMapping
    public ResponseEntity<OfferingDTO> create(@RequestBody @Valid OfferingCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(OfferingDTO.from(offeringService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfferingDTO> update(@PathVariable UUID id, @RequestBody @Valid OfferingUpdateRequest req) {
        return ResponseEntity.ok(OfferingDTO.from(offeringService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        offeringService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}

