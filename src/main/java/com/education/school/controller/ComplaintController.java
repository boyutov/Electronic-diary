package com.education.school.controller;

import com.education.school.dto.ComplaintDto;
import com.education.school.dto.ComplaintRequest;
import com.education.school.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/complaints")
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Управление жалобами")
public class ComplaintController {

    private final ComplaintService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    @Operation(summary = "Получить все жалобы (только для директора/админа)")
    public List<ComplaintDto> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Создать жалобу")
    public ComplaintDto create(@PathVariable String schoolName, @Valid @RequestBody ComplaintRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить жалобу (только автор)")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
