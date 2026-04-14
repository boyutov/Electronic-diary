package com.education.school.controller;

import com.education.school.dto.AdminRequest;
import com.education.school.dto.AdminResponse;
import com.education.school.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{schoolName}/admins")
@RequiredArgsConstructor
@Tag(name = "Admins", description = "Управление администраторами")
public class AdminController {

    private final AdminService service;

    @GetMapping
    @Operation(summary = "Получить всех администраторов")
    public List<AdminResponse> findAll(@PathVariable String schoolName) {
        return service.findAll().stream().map(AdminResponse::from).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить администратора по ID")
    public ResponseEntity<AdminResponse> findById(@PathVariable String schoolName, @PathVariable Long id) {
        return service.findById(id) != null
                ? ResponseEntity.ok(AdminResponse.from(service.findById(id)))
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Создать администратора")
    public AdminResponse create(@PathVariable String schoolName, @Valid @RequestBody AdminRequest request) {
        return AdminResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить администратора")
    public AdminResponse update(@PathVariable String schoolName, @PathVariable Long id, @Valid @RequestBody AdminRequest request) {
        return AdminResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить администратора")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
