package com.bookly.domain.availability;


import com.bookly.api.staff.dto.request.AddAvailabilityRequest;
import com.bookly.config.TenantContext;
import com.bookly.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;


    @Transactional(readOnly = true)
    public List<Availability> getByStaff(UUID staffId) {
        return availabilityRepository.findByStaffId(staffId);
    }

    public Availability addAvailability(AddAvailabilityRequest req){
        validateNoOverlap(req.staffId(),req.dayOfWeek(),req.startTime(),req.endTime());

        Availability availability = new Availability();
        availability.setTenantId(TenantContext.getTenantId());
        availability.setStaffId(req.staffId());
        availability.setDayOfWeek(req.dayOfWeek());
        availability.setStartTime(req.startTime());
        availability.setEndTime(req.endTime());

        return availabilityRepository.save(availability);
    }

    private void validateNoOverlap(UUID staffId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime){
        boolean hasOverlap = availabilityRepository.findByStaffIdAndDayOfWeek(staffId,dayOfWeek).stream()
                .anyMatch(existing -> startTime.isBefore(existing.getEndTime())
                        && endTime.isAfter(existing.getStartTime()));
        if(hasOverlap)
        {
            throw new ConflictException("Availability slot overlaps with existing schedule");
        }
    }

}
