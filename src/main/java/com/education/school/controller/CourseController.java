package com.education.school.controller;

import com.education.school.dto.CourseRequest;
import com.education.school.entity.Course;
import com.education.school.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{id}")
    @Operation(summary = "Получить курс по ID")
    public ResponseEntity<Course> findById(@PathVariable String schoolName, @PathVariable Integer id) {
        Course course = service.findById(id);
        return course != null ? ResponseEntity.ok(course) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Создать курс")
    public Course create(@PathVariable String schoolName, @Valid @RequestBody CourseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить курс")
    public Course update(@PathVariable String schoolName, @PathVariable Integer id, @Valid @RequestBody CourseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить курс")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
