package com.education.school.controller;

import com.education.school.dto.CanteenMenuDto;
import com.education.school.service.CanteenMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/canteen")
@RequiredArgsConstructor
@Tag(name = "Canteen", description = "Меню столовой")
public class CanteenMenuController {

    private final CanteenMenuService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR', 'TEACHER', 'MINISTRY')")
    @Operation(summary = "Все меню (для управления)")
    public List<CanteenMenuDto> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @GetMapping("/my")
    @Operation(summary = "Меню доступные текущему пользователю")
    public List<CanteenMenuDto> findForMe(@PathVariable String schoolName) {
        return service.findForCurrentUser();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    @Operation(summary = "Создать меню")
    public CanteenMenuDto create(@PathVariable String schoolName, @RequestBody Map<String, Object> request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    @Operation(summary = "Обновить меню")
    public CanteenMenuDto update(@PathVariable String schoolName, @PathVariable Integer id,
                                  @RequestBody Map<String, Object> request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    @Operation(summary = "Удалить меню")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
