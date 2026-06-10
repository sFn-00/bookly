package com.bookly.api.staff;

import com.bookly.api.staff.dto.request.StaffCreateRequest;
import com.bookly.api.staff.dto.response.StaffDTO;
import com.bookly.config.Roles;
import com.bookly.domain.staff.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize(Roles.OWNER_OR_STAFF)
@RequiredArgsConstructor
public class StaffAdminController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<List<StaffDTO>> getAll() {
        List<StaffDTO> staff = staffService.findAll().stream()
                .map(StaffDTO::from)
                .toList();
        return ResponseEntity.ok(staff);
    }

    @PostMapping
    @PreAuthorize(Roles.OWNER_ONLY)
    public ResponseEntity<StaffDTO> create(@Valid @RequestBody StaffCreateRequest request) {
        StaffDTO dto = StaffDTO.from(staffService.create(request));
        return ResponseEntity.created(URI.create("/api/admin/staff/" + dto.id())).body(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.OWNER_ONLY)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        staffService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
