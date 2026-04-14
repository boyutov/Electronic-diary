package com.education.school.controller;

import com.education.school.dto.AttendanceDto;
import com.education.school.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Посещаемость")
public class AttendanceController {

    private final AttendanceService service;

    /** Получить посещаемость конкретного урока */
    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN', 'DIRECTOR')")
    public List<AttendanceDto> getBySchedule(@PathVariable String schoolName,
                                              @PathVariable Integer scheduleId) {
        return service.findBySchedule(scheduleId);
    }

    /** Сохранить посещаемость урока */
    @PostMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN', 'DIRECTOR')")
    public List<AttendanceDto> saveForSchedule(@PathVariable String schoolName,
                                                @PathVariable Integer scheduleId,
                                                @RequestBody List<Map<String, Object>> records) {
        return service.saveForSchedule(scheduleId, records);
    }

    /** Моя посещаемость (ученик) */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('STUDENT')")
    public List<AttendanceDto> getMyAttendance(
            @PathVariable String schoolName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        return service.findMyAttendance(from, to);
    }

    /** Посещаемость детей (родитель) */
    @GetMapping("/children")
    @PreAuthorize("hasAuthority('PARENT')")
    public List<AttendanceDto> getChildrenAttendance(
            @PathVariable String schoolName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        return service.findChildrenAttendance(from, to);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Аналитика: вся посещаемость за период */
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    public List<AttendanceDto> getAnalytics(
            @PathVariable String schoolName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        return service.findAllByPeriod(from, to);
    }

    /** Аналитика: посещаемость конкретной группы */
    @GetMapping("/analytics/group/{groupId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    public List<AttendanceDto> getGroupAnalytics(
            @PathVariable String schoolName,
            @PathVariable Integer groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        return service.findByGroupAndPeriod(groupId, from, to);
    }
}
