package com.education.school.controller;

import com.education.school.dto.MarkDto;
import com.education.school.dto.MarkRequest;
import com.education.school.service.MarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.education.school.repository.ScheduleRepository;
import com.education.school.repository.TeacherRepository;
import com.education.school.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/marks")
@RequiredArgsConstructor
@Tag(name = "Marks", description = "Управление оценками")
public class MarkController {

    private final MarkService service;
    private final ScheduleRepository scheduleRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    /** Проверка: есть ли урок в указанную дату */
    @GetMapping("/check-lesson")
    @PreAuthorize("hasAuthority('TEACHER')")
    public Map<String, Boolean> checkLesson(@PathVariable String schoolName,
                                             @RequestParam Integer groupId,
                                             @RequestParam Integer disciplineId,
                                             @RequestParam String date) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email);
        var teacher = teacherRepository.findByUserId(user.getId()).orElse(null);
        if (teacher == null) return Map.of("hasLesson", false);
        boolean has = scheduleRepository.hasLessonOnDate(teacher.getId(), groupId, disciplineId, LocalDate.parse(date));
        return Map.of("hasLesson", has);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Поставить оценку")
    public MarkDto create(@PathVariable String schoolName, @Valid @RequestBody MarkRequest request) {
        return service.create(request);
    }

    @GetMapping("/group/{groupId}/discipline/{disciplineId}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Оценки группы по предмету")
    public List<MarkDto> findByGroupAndDiscipline(@PathVariable String schoolName,
                                                   @PathVariable Integer groupId,
                                                   @PathVariable Integer disciplineId) {
        return service.findByGroupAndDiscipline(groupId, disciplineId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Изменить оценку (требуется причина)")
    public ResponseEntity<?> update(@PathVariable String schoolName, @PathVariable Integer id,
                                     @RequestBody Map<String, Object> body) {
        try {
            Integer value = ((Number) body.get("value")).intValue();
            String reason = (String) body.get("reason");
            return ResponseEntity.ok(service.update(id, value, reason));
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Удалить оценку (требуется причина)")
    public ResponseEntity<?> delete(@PathVariable String schoolName, @PathVariable Integer id,
                                     @RequestBody Map<String, Object> body) {
        try {
            String reason = (String) body.get("reason");
            service.delete(id, reason);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
