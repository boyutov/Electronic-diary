package com.education.school.controller;

import com.education.school.dto.GroupRequest;
import com.education.school.dto.GroupResponse;
import com.education.school.dto.StudentDto;
import com.education.school.entity.GroupEntity;
import com.education.school.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Управление группами")
public class GroupController {

    private final GroupService service;

    @GetMapping
    @Operation(summary = "Получить все группы")
    public List<GroupResponse> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить группу по ID")
    public ResponseEntity<GroupResponse> findById(@PathVariable String schoolName, @PathVariable Integer id) {
        GroupEntity group = service.findById(id);
        return group != null ? ResponseEntity.ok(GroupResponse.from(group)) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Создать группу")
    public GroupEntity create(@PathVariable String schoolName, @Valid @RequestBody GroupRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить группу")
    public GroupEntity update(@PathVariable String schoolName, @PathVariable Integer id, @Valid @RequestBody GroupRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить группу")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/students")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN')")
    @Operation(summary = "Получить список учеников группы (только для куратора или админа)")
    public ResponseEntity<List<StudentDto>> getStudentsByGroupId(@PathVariable String schoolName, @PathVariable Integer groupId) {
        try {
            List<StudentDto> students = service.getStudentsByGroupId(groupId);
            return ResponseEntity.ok(students);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build(); // Forbidden
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build(); // Group not found
        }
    }
}
