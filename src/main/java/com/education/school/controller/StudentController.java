package com.education.school.controller;

import com.education.school.dto.StudentDto;
import com.education.school.dto.StudentRequest;
import com.education.school.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{schoolName}/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Управление студентами")
public class StudentController {

    private final StudentService service;

    @GetMapping
    @Operation(summary = "Получить всех студентов")
    public List<StudentDto> findAll(@PathVariable String schoolName) {
        return service.findAll().stream()
                .map(StudentDto::from)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Получить профиль текущего студента со всеми оценками")
    public StudentDto getCurrentStudent(@PathVariable String schoolName) {
        return service.getCurrentStudent();
    }

    @PostMapping
    @Operation(summary = "Создать студента")
    public StudentDto create(@PathVariable String schoolName, @Valid @RequestBody StudentRequest request) {
        return StudentDto.from(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить студента")
    public StudentDto update(@PathVariable String schoolName, @PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return StudentDto.from(service.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить по ID")
    public ResponseEntity<StudentDto> findById(@PathVariable String schoolName, @PathVariable Long id) {
        return service.findById(id) != null
                ? ResponseEntity.ok(StudentDto.from(service.findById(id)))
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить по ID")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
