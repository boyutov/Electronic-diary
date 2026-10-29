package com.education.school.controller;

import com.education.school.config.JwtService;
import com.education.school.dto.CuratorProfileDto;
import com.education.school.entity.User;
import com.education.school.dto.PasswordChangeRequest;
import com.education.school.dto.ProfileUpdateRequest;
import com.education.school.dto.UserProfileDto;
import com.education.school.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Управление профилем пользователя")
public class ProfileController {

    private final ProfileService profileService;
    private final JwtService jwtService;

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
    public UserProfileDto getCurrentUser(@PathVariable String schoolName) {
        return profileService.getCurrentUser();
    }

    @PostMapping("/password")
    @Operation(summary = "Сменить свой пароль (обязательно после входа с временным паролем)")
    public Map<String, String> changePassword(@PathVariable String schoolName, @Valid @RequestBody PasswordChangeRequest request) {
        // Смена пароля отзывает все старые токены, поэтому сразу выдаём новый
        User user = profileService.changePassword(request);
        return Map.of("token", jwtService.generateToken(user, schoolName.toLowerCase()));
    }

    @PutMapping("/me")
    @Operation(summary = "Обновить профиль пользователя")
    public UserProfileDto updateProfile(@PathVariable String schoolName, @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(request);
    }
}
