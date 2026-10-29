package com.education.school.controller;

import com.education.school.service.GradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService service;

    @GetMapping("/settings")
    public Map<String, Object> getSettings(@PathVariable String schoolName) {
        return service.getSettings(schoolName);
    }

    @PostMapping("/settings")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR')")
    public Map<String, Object> saveSettings(@PathVariable String schoolName,
                                             @RequestBody Map<String, Object> body) {
        String periodType = body.get("periodType") == null ? null : String.valueOf(body.get("periodType"));
        Integer yearStart;
        try {
            yearStart = body.get("academicYearStart") != null
                    ? Integer.valueOf(String.valueOf(body.get("academicYearStart")).trim()) : 9;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Месяц начала учебного года должен быть от 1 до 12");
        }
        String markScale = body.get("markScale") == null ? null : String.valueOf(body.get("markScale"));
        return service.saveSettings(schoolName, periodType, yearStart, markScale);
    }

    @GetMapping("/my-grades")
    @PreAuthorize("hasAuthority('STUDENT')")
    public Map<String, Object> getMyGrades(@PathVariable String schoolName) {
        return service.getStudentGrades(schoolName);
    }
}
