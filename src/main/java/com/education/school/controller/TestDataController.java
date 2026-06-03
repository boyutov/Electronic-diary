package com.education.school.controller;

import com.education.school.service.TestDataGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/test-data")
@RequiredArgsConstructor
public class TestDataController {

    private final TestDataGeneratorService testDataGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateTestData(@PathVariable String schoolName) {
        try {
            String result = testDataGeneratorService.generateTestData(schoolName);
            return ResponseEntity.ok(Map.of("result", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Ошибка генерации данных: " + e.getMessage()));
        }
    }
}