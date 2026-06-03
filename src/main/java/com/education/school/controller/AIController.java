package com.education.school.controller;

import com.education.school.service.AIAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIAnalysisService aiAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyzeSchoolData(
            @PathVariable String schoolName,
            @RequestBody Map<String, String> request) {
        
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Вопрос не может быть пустым"));
        }

        try {
            String analysis = aiAnalysisService.analyzeSchoolData(query, schoolName);
            return ResponseEntity.ok(Map.of("analysis", analysis));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Ошибка анализа: " + e.getMessage()));
        }
    }

    @PostMapping("/analyze-file")
    public ResponseEntity<Map<String, String>> analyzeFile(
            @PathVariable String schoolName,
            @RequestParam("file") MultipartFile file,
            @RequestParam("query") String query) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Файл не выбран"));
        }

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Вопрос не может быть пустым"));
        }

        try {
            byte[] fileData = file.getBytes();
            String fileName = file.getOriginalFilename();
            String analysis = aiAnalysisService.analyzeFile(fileData, fileName, query);
            
            return ResponseEntity.ok(Map.of(
                "analysis", analysis,
                "fileName", fileName,
                "fileSize", String.valueOf(file.getSize())
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Ошибка анализа файла: " + e.getMessage()));
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> executeAction(
            @PathVariable String schoolName,
            @RequestBody Map<String, String> request) {
        
        String action = request.get("action");
        if (action == null || action.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Опишите что нужно сделать"));
        }

        try {
            String result = aiAnalysisService.executeAction(action, schoolName);
            return ResponseEntity.ok(Map.of("result", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Ошибка выполнения: " + e.getMessage()));
        }
    }
}