package com.education.school.controller;

import com.education.school.dto.TeacherRequest;
import com.education.school.dto.TeacherResponse;
import com.education.school.entity.Teacher;
import com.education.school.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/teachers")
@RequiredArgsConstructor
@Tag(name = "Teachers", description = "Управление учителями")
public class TeacherController {

    private final TeacherService service;

    @GetMapping
    @Operation(summary = "Получить всех учителей")
    public List<TeacherResponse> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @GetMapping("/with-group")
    @Operation(summary = "Получить учителей с группой")
    public List<TeacherResponse> findAllWithGroup(@PathVariable String schoolName) {
        return service.findAllWithGroup();
    }

    @PostMapping
    @Operation(summary = "Создать учителя")
    public Teacher create(@PathVariable String schoolName, @Valid @RequestBody TeacherRequest request) {
        return service.create(request);
    }
}
