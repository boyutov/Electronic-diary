package com.education.school.controller;

import com.education.school.dto.MarkDto;
import com.education.school.dto.MarkRequest;
import com.education.school.service.MarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{schoolName}/marks")
@RequiredArgsConstructor
@Tag(name = "Marks", description = "Управление оценками")
public class MarkController {

    private final MarkService service;

    @PostMapping
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Поставить оценку")
    public MarkDto create(@PathVariable String schoolName, @Valid @RequestBody MarkRequest request) {
        return service.create(request);
    }
}
