package com.education.school.service;

import com.education.school.entity.GradingSettings;
import com.education.school.entity.School;
import com.education.school.repository.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GradingPeriodsTest {
    private final GradingSettingsRepository settings = mock(GradingSettingsRepository.class);
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final GradingService service = new GradingService(settings, schools, mock(StudentRepository.class),
            mock(UserRepository.class), mock(MarkRepository.class), mock(CurrentSchoolService.class));

    private static String[] p(String from, String to) { return new String[]{from, to}; }

    private GradingSettings saved(String type, List<String[]> periods) {
        when(schools.findByName("s")).thenReturn(Optional.of(new School()));
        when(settings.findBySchoolName("s")).thenReturn(Optional.empty());
        service.saveSettings("s", type, null, periods);
        var captor = org.mockito.ArgumentCaptor.forClass(GradingSettings.class);
        verify(settings).save(captor.capture());
        return captor.getValue();
    }

    @Test void defaultQuartersFollowSchoolCalendarWithoutSummer() {
        int year = LocalDate.now().getMonthValue() >= 9 ? LocalDate.now().getYear() - 1 : LocalDate.now().getYear() - 2;
        var periods = service.buildPeriods(null, "QUARTER", LocalDate.of(year, 9, 15));
        var first = periods.subList(0, 4);
        assertEquals(List.of(year + "-09-01", year + "-11-01", (year + 1) + "-01-01", (year + 1) + "-04-01"),
                first.stream().map(x -> x.get("from")).toList());
        assertEquals(List.of(year + "-10-31", year + "-12-31", (year + 1) + "-03-31", (year + 1) + "-05-31"),
                first.stream().map(x -> x.get("to")).toList());
        assertEquals("1 четверть " + year + "/" + (year + 1), first.get(0).get("name"));
    }

    @Test void customPeriodsAreStoredAndUsed() {
        var s = saved("SEMESTER", List.of(p("09-02", "12-27"), p("01-13", "05-25")));
        assertEquals(9, s.getAcademicYearStart());
        int year = LocalDate.now().getYear() - 2;
        var periods = service.buildPeriods(s, "SEMESTER", LocalDate.of(year, 10, 1));
        assertEquals(year + "-09-02", periods.get(0).get("from"));
        assertEquals((year + 1) + "-01-13", periods.get(1).get("from"));
        assertEquals((year + 1) + "-05-25", periods.get(1).get("to"));
    }

    @Test void rejectsOverlappingReversedOrIncompletePeriods() {
        when(schools.findByName("s")).thenReturn(Optional.of(new School()));
        when(settings.findBySchoolName("s")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.saveSettings("s", "QUARTER", null,
                List.of(p("09-01", "11-05"), p("11-01", "12-31"), p("01-10", "03-20"), p("04-01", "05-31"))));
        assertThrows(IllegalArgumentException.class, () -> service.saveSettings("s", "QUARTER", null,
                List.of(p("09-01", "10-31"), p("11-01", "10-15"), p("01-10", "03-20"), p("04-01", "05-31"))));
        assertThrows(IllegalArgumentException.class, () -> service.saveSettings("s", "SEMESTER", null,
                Collections.singletonList(p("09-01", "12-31"))));
        assertThrows(IllegalArgumentException.class, () -> service.saveSettings("s", "YEAR", null,
                Collections.singletonList(p("02-30", "05-31"))));
        verify(settings, never()).save(any());
    }

    @Test void emptyListRestoresDefaultCalendar() {
        var s = new GradingSettings();
        s.setCustomPeriods("{\"QUARTER\":[[\"09-02\",\"10-30\"],[\"11-05\",\"12-28\"],[\"01-10\",\"03-20\"],[\"04-01\",\"05-25\"]]}");
        when(schools.findByName("s")).thenReturn(Optional.of(new School()));
        when(settings.findBySchoolName("s")).thenReturn(Optional.of(s));
        service.saveSettings("s", "QUARTER", null, List.of());
        assertNull(s.getCustomPeriods());
    }
}
