package com.education.school.controller;

import com.education.school.dto.PollRequest;
import com.education.school.entity.Poll;
import com.education.school.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/polls")
@RequiredArgsConstructor
@Tag(name = "Polls", description = "Управление голосованиями")
public class PollController {

    private final PollService service;

    @GetMapping
    @Operation(summary = "Получить все голосования")
    public List<Poll> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить голосование по ID")
    public ResponseEntity<Poll> findById(@PathVariable String schoolName, @PathVariable Integer id) {
        Poll poll = service.findById(id);
        return poll != null ? ResponseEntity.ok(poll) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Создать голосование")
    public Poll create(@PathVariable String schoolName, @Valid @RequestBody PollRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить голосование")
    public Poll update(@PathVariable String schoolName, @PathVariable Integer id, @Valid @RequestBody PollRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить голосование")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
