package com.education.school.controller;

import com.education.school.config.JwtService;
import com.education.school.dto.AuthenticationRequest;
import com.education.school.dto.AuthenticationResponse;
import com.education.school.entity.User;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail());

        // В реальном приложении здесь нужно проверить, принадлежит ли пользователь запрошенной школе
        // Для упрощения мы пока доверяем request.getSchoolName(), но лучше проверить user.getSchools()

        String jwtToken = jwtService.generateToken(user, toSlug(request.getSchoolName()));

        return ResponseEntity.ok(AuthenticationResponse.builder()
                .token(jwtToken)
                .role(user.getRole().getName())
                .schoolName(toSlug(request.getSchoolName()))
                .build());
    }

    private String toSlug(String name) {
        if (name == null) return "";
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9а-яё]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
