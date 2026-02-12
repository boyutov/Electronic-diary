package com.education.school.controller;

import com.education.school.dto.AdminRequest;
import com.education.school.entity.User;
import com.education.school.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{schoolName}/admins")
@RequiredArgsConstructor
@Tag(name = "Admins", description = "Управление администраторами")
public class AdminController {

    private final AdminService service;

    @PostMapping
    @Operation(summary = "Создать администратора")
    public User create(@PathVariable String schoolName, @Valid @RequestBody AdminRequest request) {
        return service.create(request);
    }
}
