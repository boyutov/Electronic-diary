package com.education.school.controller;

import com.education.school.entity.Discipline;
import com.education.school.service.DisciplineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
    public List<Discipline> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Создать предмет")
    public Discipline create(@PathVariable String schoolName, @RequestBody Discipline discipline) {
        return service.create(discipline);
    }
}
