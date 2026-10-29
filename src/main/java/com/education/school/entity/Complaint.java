package com.education.school.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

// Жалоба — может быть анонимной или именной
@Setter
@Getter
@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Школа-владелец записи (Multi-Tenancy): Hibernate сам подставляет текущую школу при сохранении
    // и добавляет "school_id = :текущая школа" во все запросы, включая findById
    @TenantId
    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;  // текст жалобы

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // Мягкое удаление — директор может скрыть жалобу не удаляя из БД
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // Анонимная жалоба: true — автор скрыт от просматривающих
    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous;

    // Автор (null если анонимная, но в БД всё равно сохраняется для внутренних нужд)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User authorUser;
}
