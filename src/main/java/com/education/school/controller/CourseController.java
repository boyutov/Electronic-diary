package com.education.school.controller;

import com.education.school.dto.CourseRequest;
import com.education.school.entity.Course;
import com.education.school.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Управление курсами")
public class CourseController {

    private final CourseService service;

    @GetMapping
    @Operation(summary = "Получить все курсы")
    public List<Course> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Создать курс")
    public Course create(@PathVariable String schoolName, @Valid @RequestBody CourseRequest request) {
        return service.create(request);
    }
}
