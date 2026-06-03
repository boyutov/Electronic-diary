package com.education.school.controller;

import com.education.school.service.SchoolDataGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/ai-generator")
@RequiredArgsConstructor
public class AIDataGeneratorController {

    private final SchoolDataGeneratorService generatorService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateWithAI(
            @PathVariable String schoolName,
            @RequestBody Map<String, String> request) {

        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Опишите что нужно создать"));
        }
        try {
            String result = generatorService.generateFromPrompt(prompt, schoolName);
            return ResponseEntity.ok(Map.of("result", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}