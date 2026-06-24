package com.education.school.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

// Школа — центральная сущность Multi-Tenancy
// Все данные (пользователи, группы, оценки) привязаны к конкретной школе
@Setter
@Getter
@Entity
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;             // уникальное имя-slug: "schoola", "gymnasium5" (используется в URL)

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;     // контактный email при регистрации

    @Column(name = "contact_phone")
    private String contactPhone;

    // Хэш временного пароля активации — выдаётся при покупке, используется один раз
    @Column(name = "access_password_hash")
    private String accessPasswordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Директор школы — первый пользователь, создаётся при активации
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_user_id")
    private User directorUser;

    // Обратная сторона связи Many-to-Many с пользователями (основная сторона в User.schools)
    @ManyToMany(mappedBy = "schools")
    private Set<User> users = new HashSet<>();
}
