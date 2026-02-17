package com.education.school.controller;

import com.education.school.dto.DisciplineDto;
import com.education.school.entity.Discipline;
import com.education.school.service.DisciplineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/disciplines")
@RequiredArgsConstructor
@Tag(name = "Disciplines", description = "Управление предметами")
public class DisciplineController {

    private final DisciplineService service;

    @GetMapping
    @Operation(summary = "Получить все предметы")
    public List<DisciplineDto> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить предмет по ID")
    public ResponseEntity<DisciplineDto> findById(@PathVariable String schoolName, @PathVariable Integer id) {
        Discipline discipline = service.findById(id);
        return discipline != null ? ResponseEntity.ok(DisciplineDto.from(discipline)) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Создать предмет")
    public DisciplineDto create(@PathVariable String schoolName, @RequestBody Discipline discipline) {
        return DisciplineDto.from(service.create(discipline));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить предмет")
    public DisciplineDto update(@PathVariable String schoolName, @PathVariable Integer id, @RequestBody Discipline discipline) {
        return DisciplineDto.from(service.update(id, discipline));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить предмет")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
