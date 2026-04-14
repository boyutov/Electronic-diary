package com.education.school.controller;

import com.education.school.dto.ExamDto;
import com.education.school.service.ExamService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/exams")
@RequiredArgsConstructor
@Tag(name = "Exams", description = "Экзамены и тесты")
public class ExamController {

    private final ExamService service;

    @GetMapping("/my")
    public List<ExamDto> findForMe(@PathVariable String schoolName) {
        return service.findForCurrentUser();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    public List<ExamDto> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN', 'DIRECTOR')")
    public ExamDto create(@PathVariable String schoolName, @RequestBody Map<String, Object> req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN', 'DIRECTOR')")
    public ExamDto update(@PathVariable String schoolName, @PathVariable Integer id,
                           @RequestBody Map<String, Object> req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN', 'DIRECTOR')")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
