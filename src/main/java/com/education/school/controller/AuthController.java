package com.education.school.controller;

import com.education.school.config.JwtService;
import com.education.school.dto.AuthenticationRequest;
import com.education.school.dto.AuthenticationResponse;
import com.education.school.entity.User;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
        String schoolSlug = toSlug(request.getSchoolName());

        // Multi-Tenancy: токен выдаётся только для школы, к которой пользователь реально привязан
        boolean belongsToSchool = !schoolSlug.isEmpty() && user.getSchools().stream()
                .anyMatch(school -> school.getName().equalsIgnoreCase(schoolSlug));
        if (!belongsToSchool) {
            throw new BadCredentialsException("User does not belong to school: " + schoolSlug);
        }

        String jwtToken = jwtService.generateToken(user, schoolSlug);

        return ResponseEntity.ok(AuthenticationResponse.builder()
                .token(jwtToken)
                .role(user.getRole().getName())
                .schoolName(schoolSlug)
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
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
