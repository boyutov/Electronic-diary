package com.education.school.controller;

import com.education.school.dto.NotificationDto;
import com.education.school.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/{schoolName}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public List<NotificationDto> findMy(@PathVariable String schoolName) {
        return service.findMy();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@PathVariable String schoolName) {
        return Map.of("count", service.countUnread());
    }

    @PostMapping("/read-all")
    public void markAllRead(@PathVariable String schoolName) {
        service.markAllRead();
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable String schoolName, @PathVariable Long id) {
        service.markRead(id);
    }
}
