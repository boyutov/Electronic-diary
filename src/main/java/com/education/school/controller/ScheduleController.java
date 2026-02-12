package com.education.school.controller;

import com.education.school.dto.ScheduleRequest;
import com.education.school.entity.Schedule;
import com.education.school.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Управление расписанием")
public class ScheduleController {

    private final ScheduleService service;

    @GetMapping
    @Operation(summary = "Получить все расписание")
    public List<Schedule> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Добавить урок в расписание")
    public Schedule create(@PathVariable String schoolName, @Valid @RequestBody ScheduleRequest request) {
        return service.create(request);
    }
}
