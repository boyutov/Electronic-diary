package com.education.school.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

// Уведомление для пользователя — создаётся автоматически при выставлении оценок, изменениях и т.д.
@Setter
@Getter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Школа-владелец записи (Multi-Tenancy): Hibernate сам подставляет текущую школу при сохранении
    // и добавляет "school_id = :текущая школа" во все запросы, включая findById
    @TenantId
    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    // Кому адресовано уведомление
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String type;   // тип: "MARK", "NEWS", "SCHEDULE" и т.д.

    @Column(nullable = false)
    private String title;  // заголовок уведомления

    @Column(columnDefinition = "TEXT")
    private String body;   // текст уведомления

    // Флаг прочитан/непрочитан — используется для счётчика непрочитанных в шапке
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    private String link;  // ссылка для перехода при клике на уведомление
}
