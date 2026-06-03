package com.education.school.controller;

import com.education.school.repository.UserRepository;
import com.education.school.service.AiAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService service;
    private final UserRepository userRepository;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR', 'TEACHER')")
    public Map<String, Object> chat(@PathVariable String schoolName,
                                     @RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return Map.of("reply", "Сообщение не может быть пустым", "success", false);
        }
        return service.chat(schoolName, message);
    }

    @PostMapping("/clear")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DIRECTOR', 'TEACHER')")
    public Map<String, Boolean> clearHistory(@PathVariable String schoolName) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email);
        service.clearHistory(user.getId());
        return Map.of("success", true);
    }
}
