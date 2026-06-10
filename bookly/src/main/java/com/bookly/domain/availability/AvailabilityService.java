package com.bookly.domain.availability;


import com.bookly.api.staff.dto.request.AddAvailabilityRequest;
import com.bookly.config.TenantContext;
import com.bookly.exception.ConflictException;
import com.bookly.util.TimeUtils;
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

    @Transactional(readOnly = true)
    public List<Availability> getByStaffAndDay(UUID staffId, DayOfWeek dayOfWeek) {
        return availabilityRepository.findByStaffIdAndDayOfWeek(staffId, dayOfWeek);
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
        boolean hasOverlap = availabilityRepository.findByStaffIdAndDayOfWeek(staffId, dayOfWeek).stream()
                .anyMatch(existing -> TimeUtils.overlaps(startTime, endTime, existing.getStartTime(), existing.getEndTime()));
        if(hasOverlap)
        {
            throw new ConflictException("Availability slot overlaps with existing schedule");
        }
    }

}
