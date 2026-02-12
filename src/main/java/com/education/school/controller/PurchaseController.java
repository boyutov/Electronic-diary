package com.education.school.controller;

import com.education.school.dto.PurchaseRequest;
import com.education.school.dto.PurchaseResponse;
import com.education.school.dto.SchoolRegistrationRequest;
import com.education.school.service.SchoolOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {

    private final SchoolOnboardingService schoolOnboardingService;

    public PurchaseController(SchoolOnboardingService schoolOnboardingService) {
        this.schoolOnboardingService = schoolOnboardingService;
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> purchase(@Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.ok(schoolOnboardingService.createSchoolAccount(request));
    }

    @PostMapping("/register")
    public ResponseEntity<PurchaseResponse> register(@Valid @RequestBody SchoolRegistrationRequest request) {
        return ResponseEntity.ok(schoolOnboardingService.completeSchoolRegistration(request));
    }

    @PostMapping("/calculate")
    public ResponseEntity<Double> calculate(@RequestBody PurchaseRequest request) {
        double price = schoolOnboardingService.calculatePrice(
            request.studentCount() != null ? request.studentCount() : 0,
            request.durationMonths() != null ? request.durationMonths() : 0,
            request.promoCode()
        );
        return ResponseEntity.ok(price);
    }
}
