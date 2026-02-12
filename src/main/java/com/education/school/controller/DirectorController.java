package com.education.school.controller;

import com.education.school.dto.DirectorRequest;
import com.education.school.entity.User;
import com.education.school.service.DirectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{schoolName}/directors")
@RequiredArgsConstructor
@Tag(name = "Directors", description = "Управление директорами")
public class DirectorController {

    private final DirectorService service;

    @PostMapping
    @Operation(summary = "Создать директора")
    public User create(@PathVariable String schoolName, @Valid @RequestBody DirectorRequest request) {
        return service.create(request);
    }
}
