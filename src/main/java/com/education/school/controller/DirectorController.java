package com.education.school.controller;

import com.education.school.dto.DirectorRequest;
import com.education.school.dto.DirectorResponse;
import com.education.school.service.DirectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{schoolName}/directors")
@RequiredArgsConstructor
@Tag(name = "Directors", description = "Управление директорами")
public class DirectorController {

    private final DirectorService service;

    @GetMapping
    @Operation(summary = "Получить всех директоров")
    public List<DirectorResponse> findAll(@PathVariable String schoolName) {
        return service.findAll().stream().map(DirectorResponse::from).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить директора по ID")
    public ResponseEntity<DirectorResponse> findById(@PathVariable String schoolName, @PathVariable Long id) {
        return service.findById(id) != null
                ? ResponseEntity.ok(DirectorResponse.from(service.findById(id)))
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Создать директора")
    public DirectorResponse create(@PathVariable String schoolName, @Valid @RequestBody DirectorRequest request) {
        return DirectorResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить директора")
    public DirectorResponse update(@PathVariable String schoolName, @PathVariable Long id, @Valid @RequestBody DirectorRequest request) {
        return DirectorResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить директора")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
