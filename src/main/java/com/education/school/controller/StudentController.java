package com.education.school.controller;

import com.education.school.dto.StudentRequest;
import com.education.school.entity.Student;
import com.education.school.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Управление студентами")
public class StudentController {

    private final StudentService service;

    @GetMapping
    @Operation(summary = "Получить всех студентов")
    public List<Student> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Создать студента")
    public Student create(@PathVariable String schoolName, @Valid @RequestBody StudentRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить по ID")
    public ResponseEntity<Student> findById(@PathVariable String schoolName, @PathVariable Long id) {
        Student student = service.findById(id);
        return student != null ? ResponseEntity.ok(student) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить по ID")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
