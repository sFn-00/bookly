package com.bookly.domain.availability;

import com.bookly.api.staff.dto.request.AddAvailabilityRequest;
import com.bookly.config.TenantContext;
import com.bookly.exception.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock AvailabilityRepository availabilityRepository;
    @InjectMocks AvailabilityService availabilityService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenant(tenantId.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void addAvailability_noOverlap_savesAndReturns() {
        AddAvailabilityRequest req = new AddAvailabilityRequest(
                staffId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        when(availabilityRepository.findByStaffIdAndDayOfWeek(staffId, DayOfWeek.MONDAY))
                .thenReturn(List.of());
        Availability saved = buildAvailability(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        when(availabilityRepository.save(any())).thenReturn(saved);

        Availability result = availabilityService.addAvailability(req);

        ArgumentCaptor<Availability> captor = ArgumentCaptor.forClass(Availability.class);
        verify(availabilityRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().getStaffId()).isEqualTo(staffId);
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void addAvailability_overlapping_throwsConflict() {
        Availability existing = buildAvailability(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(availabilityRepository.findByStaffIdAndDayOfWeek(staffId, DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));

        AddAvailabilityRequest req = new AddAvailabilityRequest(
                staffId, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(14, 0));

        assertThatThrownBy(() -> availabilityService.addAvailability(req))
                .isInstanceOf(ConflictException.class);

        verify(availabilityRepository, never()).save(any());
    }

    @Test
    void addAvailability_adjacentSlot_doesNotOverlap() {
        Availability existing = buildAvailability(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(availabilityRepository.findByStaffIdAndDayOfWeek(staffId, DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));
        when(availabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AddAvailabilityRequest req = new AddAvailabilityRequest(
                staffId, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(17, 0));

        assertThatCode(() -> availabilityService.addAvailability(req))
                .doesNotThrowAnyException();
    }

    @Test
    void addAvailability_differentDay_doesNotCheckOverlap() {
        when(availabilityRepository.findByStaffIdAndDayOfWeek(staffId, DayOfWeek.TUESDAY))
                .thenReturn(List.of());
        when(availabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AddAvailabilityRequest req = new AddAvailabilityRequest(
                staffId, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));

        assertThatCode(() -> availabilityService.addAvailability(req))
                .doesNotThrowAnyException();
    }

    @Test
    void getByStaff_returnsAllSlotsForStaff() {
        List<Availability> slots = List.of(
                buildAvailability(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                buildAvailability(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))
        );
        when(availabilityRepository.findByStaffId(staffId)).thenReturn(slots);

        List<Availability> result = availabilityService.getByStaff(staffId);

        assertThat(result).hasSize(2);
    }

    private Availability buildAvailability(DayOfWeek day, LocalTime start, LocalTime end) {
        Availability a = new Availability();
        a.setId(UUID.randomUUID());
        a.setTenantId(tenantId);
        a.setStaffId(staffId);
        a.setDayOfWeek(day);
        a.setStartTime(start);
        a.setEndTime(end);
        return a;
    }
}
