package com.education.school.controller;

import com.education.school.service.GradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        String markScale = body.get("markScale") == null ? null : String.valueOf(body.get("markScale"));
        // periods: [["09-01","10-31"], ...] — даты периодов выбранного типа; [] — вернуть школьный календарь
        List<String[]> periods = null;
        if (body.get("periods") instanceof List<?> list) {
            periods = new java.util.ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof List<?> pair) || pair.size() != 2) {
                    throw new IllegalArgumentException("Каждый период — пара дат «начало, конец»");
                }
                periods.add(new String[]{String.valueOf(pair.get(0)), String.valueOf(pair.get(1))});
            }
        }
        return service.saveSettings(schoolName, periodType, markScale, periods);
    }

    @GetMapping("/periods")
    public Map<String, Object> getPeriods(@PathVariable String schoolName) {
        return service.currentPeriods();
    }

    @GetMapping("/my-grades")
    @PreAuthorize("hasAuthority('STUDENT')")
    public Map<String, Object> getMyGrades(@PathVariable String schoolName) {
        return service.getStudentGrades(schoolName);
    }
}
