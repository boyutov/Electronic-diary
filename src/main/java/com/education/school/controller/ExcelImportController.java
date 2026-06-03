package com.education.school.controller;

import com.education.school.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/excel")
@RequiredArgsConstructor
public class ExcelImportController {

    private final ExcelImportService excelImportService;

    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> preview(
            @PathVariable String schoolName,
            @RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = excelImportService.preview(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Ошибка анализа файла: " + e.getMessage()));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importStudents(
            @PathVariable String schoolName,
            @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> result = excelImportService.importStudents(body);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Ошибка импорта: " + e.getMessage()));
        }
    }
}