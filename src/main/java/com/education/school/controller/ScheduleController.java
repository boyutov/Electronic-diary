package com.education.school.controller;

import com.education.school.dto.ScheduleRequest;
import com.education.school.dto.ScheduleResponse;
import com.education.school.entity.Schedule;
import com.education.school.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Управление расписанием")
public class ScheduleController {

    private final ScheduleService service;

    @GetMapping
    @Operation(summary = "Получить все расписание")
    public List<ScheduleResponse> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Добавить урок в расписание")
    public ScheduleResponse create(@PathVariable String schoolName, @Valid @RequestBody ScheduleRequest request) {
        return ScheduleResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    @Operation(summary = "Обновить урок")
    public ScheduleResponse update(@PathVariable String schoolName, @PathVariable Integer id, @Valid @RequestBody ScheduleRequest request) {
        return ScheduleResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    @Operation(summary = "Удалить урок")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my/today")
    @Operation(summary = "Получить мое расписание на сегодня")
    public List<ScheduleResponse> getMyScheduleForToday(@PathVariable String schoolName) {
        return service.getMyScheduleForToday();
    }

    @GetMapping("/my/period")
    @Operation(summary = "Получить мое расписание за период")
    public List<ScheduleResponse> getMyScheduleForPeriod(
            @PathVariable String schoolName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return service.getMyScheduleForPeriod(start, end);
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    @Operation(summary = "Получить расписание группы за период (для админа)")
    public List<ScheduleResponse> getScheduleByGroupId(
            @PathVariable String schoolName,
            @PathVariable Integer groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return service.getScheduleByGroupId(groupId, start, end);
    }
}
