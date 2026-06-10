package com.bookly.domain.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {

    List<Availability> findByStaffIdAndDayOfWeek(UUID staffId, DayOfWeek dayOfWeek);

    List<Availability> findByStaffId(UUID staffId);
}
