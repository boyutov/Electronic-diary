package com.education.school.controller;

import com.education.school.dto.DirectorRequest;
import com.education.school.entity.User;
import com.education.school.service.DirectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/directors")
@RequiredArgsConstructor
@Tag(name = "Directors", description = "Управление директорами")
public class DirectorController {

    private final DirectorService service;

    @GetMapping
    @Operation(summary = "Получить всех директоров")
    public List<User> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить директора по ID")
    public ResponseEntity<User> findById(@PathVariable String schoolName, @PathVariable Long id) {
        User user = service.findById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Создать директора")
    public User create(@PathVariable String schoolName, @Valid @RequestBody DirectorRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить директора")
    public User update(@PathVariable String schoolName, @PathVariable Long id, @Valid @RequestBody DirectorRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить директора")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
