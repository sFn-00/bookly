package com.bookly.api.staff;

import com.bookly.api.staff.dto.request.AddAvailabilityRequest;
import com.bookly.api.staff.dto.response.AvailabilityDTO;
import com.bookly.config.Roles;
import com.bookly.domain.availability.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/staff/{staffId}/availability")
@PreAuthorize(Roles.OWNER_OR_STAFF)
@RequiredArgsConstructor
public class AvailabilityAdminController {

    private final AvailabilityService availabilityService;

    @GetMapping
    public ResponseEntity<List<AvailabilityDTO>> getByStaff(@PathVariable UUID staffId) {
        List<AvailabilityDTO> slots = availabilityService.getByStaff(staffId).stream()
                .map(AvailabilityDTO::from)
                .toList();
        return ResponseEntity.ok(slots);
    }

    @PostMapping
    @PreAuthorize(Roles.OWNER_ONLY)
    public ResponseEntity<AvailabilityDTO> add(
            @PathVariable UUID staffId,
            @Valid @RequestBody AddAvailabilityRequest request
    ) {
        AvailabilityDTO dto = AvailabilityDTO.from(availabilityService.addAvailability(request));
        return ResponseEntity.created(URI.create("/api/admin/staff/" + staffId + "/availability/" + dto.id())).body(dto);
    }
}
