package com.education.school.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Новость школы — публикуется учителем или администратором
@Setter
@Getter
@Entity
@Table(name = "news")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Школа-владелец записи (Multi-Tenancy): Hibernate сам подставляет текущую школу при сохранении
    // и добавляет "school_id = :текущая школа" во все запросы, включая findById
    @TenantId
    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    @Column(nullable = false)
    private String title;  // заголовок

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;   // содержание (длинный текст)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Учитель который опубликовал новость (если публиковал учитель)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    // Пользователь который создал новость (может быть любой роли: ADMIN, DIRECTOR, TEACHER)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;
}
