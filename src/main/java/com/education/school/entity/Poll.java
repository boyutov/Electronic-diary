package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Опрос — создаётся для сбора мнений участников школы
@Setter
@Getter
@Entity
@Table(name = "polls")
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private Boolean active;  // опрос открыт или закрыт

    // Роли которые могут голосовать: "STUDENT,TEACHER,PARENT" (хранится как строка)
    @Column(name = "allowed_roles")
    private String allowedRoles;

    // Варианты ответа — cascade: при удалении опроса удаляются и варианты
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PollOption> options = new HashSet<>();

    // Голоса — cascade: при удалении опроса удаляются и голоса
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PollVote> votes = new HashSet<>();
}
