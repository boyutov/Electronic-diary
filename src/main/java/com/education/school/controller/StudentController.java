package com.education.school.controller;

import com.education.school.entity.Student;
import com.education.school.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Управление студентами")
public class StudentController {

    private final StudentService service;

    @GetMapping
    @Operation(summary = "Получить всех студентов")
    public List<Student> findAll() {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Создать студента")
    public Student create(@RequestBody Student student) {
        return service.save(student);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить по ID")
    public ResponseEntity<Student> findById(@PathVariable Long id) {
        Student student = service.findById(id);
        return student != null ? ResponseEntity.ok(student) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить по ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}