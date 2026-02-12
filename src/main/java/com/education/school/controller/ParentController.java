package com.education.school.controller;

import com.education.school.dto.ParentRequest;
import com.education.school.entity.Parent;
import com.education.school.service.ParentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    @Operation(summary = "Создать родителя")
    public Parent create(@PathVariable String schoolName, @Valid @RequestBody ParentRequest request) {
        return service.create(request);
    }
}
