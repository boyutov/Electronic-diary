package com.education.school.controller;

import com.education.school.dto.CuratorProfileDto;
import com.education.school.dto.ProfileUpdateRequest;
import com.education.school.entity.User;
import com.education.school.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{schoolName}/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Управление профилем пользователя")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/curator")
    @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN')")
    @Operation(summary = "Получить профиль куратора")
    public ResponseEntity<CuratorProfileDto> getCuratorProfile(@PathVariable String schoolName) {
        return profileService.getCuratorProfile()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    @Operation(summary = "Получить текущего пользователя")
    public User getCurrentUser(@PathVariable String schoolName) {
        return profileService.getCurrentUser();
    }

    @PutMapping("/me")
    @Operation(summary = "Обновить профиль пользователя")
    public User updateProfile(@PathVariable String schoolName, @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(request);
    }
}
