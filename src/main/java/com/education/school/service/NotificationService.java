package com.education.school.service;

import com.education.school.dto.NotificationDto;
import com.education.school.entity.Notification;
import com.education.school.entity.User;
import com.education.school.repository.NotificationRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /** Создать уведомление для конкретного пользователя */
    @Transactional
    public void send(Long userId, String type, String title, String body, String link) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setLink(link);
        n.setIsRead(false);
        n.setCreatedAt(OffsetDateTime.now());
        notificationRepository.save(n);
    }

    /** Создать уведомление для списка пользователей */
    @Transactional
    public void sendToAll(List<Long> userIds, String type, String title, String body, String link) {
        userIds.forEach(id -> send(id, type, title, body, link));
    }

    /** Уведомления текущего пользователя */
    @Transactional(readOnly = true)
    public List<NotificationDto> findMy() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(NotificationDto::from).collect(Collectors.toList());
    }

    /** Количество непрочитанных */
    @Transactional(readOnly = true)
    public long countUnread() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        return notificationRepository.countUnreadByUserId(user.getId());
    }

    /** Пометить все как прочитанные */
    @Transactional
    public void markAllRead() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        notificationRepository.markAllReadByUserId(user.getId());
    }

    /** Пометить одно как прочитанное */
    @Transactional
    public void markRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }
}
