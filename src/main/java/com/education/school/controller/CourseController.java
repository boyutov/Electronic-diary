package com.education.school.controller;

import com.education.school.dto.CourseRequest;
import com.education.school.dto.CourseResponse;
import com.education.school.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{schoolName}/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Управление курсами")
public class CourseController {

    private final CourseService service;

    @GetMapping
    @Operation(summary = "Все курсы")
    public List<CourseResponse> findAll(@PathVariable String schoolName) {
        return service.findAll().stream().map(CourseResponse::from).collect(Collectors.toList());
    }

    @GetMapping("/for-student")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Все курсы с флагом записи для студента")
    public List<CourseResponse> findAllForStudent(@PathVariable String schoolName) {
        return service.findAllForStudent();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Мои курсы (учитель) со списком студентов")
    public List<CourseResponse> findMyCourses(@PathVariable String schoolName) {
        return service.findMyCoursesAsTeacher();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Курс по ID")
    public ResponseEntity<CourseResponse> findById(@PathVariable String schoolName, @PathVariable Integer id) {
        return service.findById(id) != null
                ? ResponseEntity.ok(CourseResponse.from(service.findById(id)))
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Записаться на курс")
    public ResponseEntity<?> enroll(@PathVariable String schoolName, @PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.enroll(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/enroll")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Отписаться от курса")
    public ResponseEntity<?> unenroll(@PathVariable String schoolName, @PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.unenroll(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "Создать курс")
    public CourseResponse create(@PathVariable String schoolName, @Valid @RequestBody CourseRequest request) {
        return CourseResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить курс")
    public CourseResponse update(@PathVariable String schoolName, @PathVariable Integer id,
                                  @Valid @RequestBody CourseRequest request) {
        return CourseResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить курс")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
