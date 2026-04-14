package com.education.school.controller;

import com.education.school.dto.PollRequest;
import com.education.school.dto.PollResponse;
import com.education.school.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{schoolName}/polls")
@RequiredArgsConstructor
@Tag(name = "Polls", description = "Управление голосованиями")
public class PollController {

    private final PollService service;

    @GetMapping
    @Operation(summary = "Получить все голосования (для админа)")
    public List<PollResponse> findAll(@PathVariable String schoolName) {
        return service.findAll().stream().map(PollResponse::from).collect(Collectors.toList());
    }

    @GetMapping("/my")
    @Operation(summary = "Получить голосования доступные текущему пользователю")
    public List<PollResponse> findForMe(@PathVariable String schoolName) {
        return service.findAllForCurrentUser();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить голосование по ID")
    public ResponseEntity<PollResponse> findById(@PathVariable String schoolName, @PathVariable Integer id) {
        return service.findById(id) != null
                ? ResponseEntity.ok(PollResponse.from(service.findById(id)))
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Создать голосование")
    public PollResponse create(@PathVariable String schoolName, @Valid @RequestBody PollRequest request) {
        return PollResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить голосование")
    public PollResponse update(@PathVariable String schoolName, @PathVariable Integer id, @Valid @RequestBody PollRequest request) {
        return PollResponse.from(service.update(id, request));
    }

    @PostMapping("/{id}/vote")
    @Operation(summary = "Проголосовать")
    public ResponseEntity<?> vote(@PathVariable String schoolName, @PathVariable Integer id,
                                  @RequestBody Map<String, Integer> body) {
        try {
            return ResponseEntity.ok(service.vote(id, body.get("optionId")));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить голосование")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
