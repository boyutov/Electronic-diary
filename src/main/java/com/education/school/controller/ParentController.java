package com.education.school.controller;

import com.education.school.dto.ParentRequest;
import com.education.school.dto.StudentDto;
import com.education.school.entity.Parent;
import com.education.school.service.ParentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/parents")
@RequiredArgsConstructor
@Tag(name = "Parents", description = "Управление родителями")
public class ParentController {

    private final ParentService service;

    @GetMapping
    @Operation(summary = "Получить всех родителей")
    public List<Parent> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить родителя по ID")
    public ResponseEntity<Parent> findById(@PathVariable String schoolName, @PathVariable Integer id) {
        Parent parent = service.findById(id);
        return parent != null ? ResponseEntity.ok(parent) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Создать родителя")
    public Parent create(@PathVariable String schoolName, @Valid @RequestBody ParentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить родителя")
    public Parent update(@PathVariable String schoolName, @PathVariable Integer id, @Valid @RequestBody ParentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить родителя")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my/children")
    @Operation(summary = "Получить моих детей")
    public List<StudentDto> getMyChildren(@PathVariable String schoolName) {
        return service.getMyChildren();
    }
}
