package com.education.school.controller;

import com.education.school.dto.PollRequest;
import com.education.school.entity.Poll;
import com.education.school.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    @Operation(summary = "Создать голосование")
    public Poll create(@PathVariable String schoolName, @Valid @RequestBody PollRequest request) {
        return service.create(request);
    }
}
