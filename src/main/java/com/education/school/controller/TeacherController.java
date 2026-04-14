package com.education.school.controller;

import com.education.school.dto.TeacherRequest;
import com.education.school.dto.TeacherResponse;
import com.education.school.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/teachers")
@RequiredArgsConstructor
@Tag(name = "Teachers", description = "Управление учителями")
public class TeacherController {

    private final TeacherService service;

    @GetMapping
    @Operation(summary = "Получить всех учителей")
    public List<TeacherResponse> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @GetMapping("/with-group")
    @Operation(summary = "Получить учителей с группой")
    public List<TeacherResponse> findAllWithGroup(@PathVariable String schoolName) {
        return service.findAllWithGroup();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить учителя по ID")
    public ResponseEntity<TeacherResponse> findById(@PathVariable String schoolName, @PathVariable Integer id) {
        return service.findById(id) != null
                ? ResponseEntity.ok(TeacherResponse.from(service.findById(id)))
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Создать учителя")
    public TeacherResponse create(@PathVariable String schoolName, @Valid @RequestBody TeacherRequest request) {
        return TeacherResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить учителя")
    public TeacherResponse update(@PathVariable String schoolName, @PathVariable Integer id, @Valid @RequestBody TeacherRequest request) {
        return TeacherResponse.from(service.update(id, request));
    }

    @GetMapping("/my/groups-disciplines")
    @Operation(summary = "Получить группы и предметы текущего учителя")
    public List<java.util.Map<String, Object>> getMyGroupsWithDisciplines(@PathVariable String schoolName) {
        return service.getMyGroupsWithDisciplines();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить учителя")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
