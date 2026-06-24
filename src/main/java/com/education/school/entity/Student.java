package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

// Ученик — расширяет пользователя дополнительными данными
// Паттерн: User содержит данные для аутентификации, Student — данные предметной области
@Setter
@Getter
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Один-к-одному с User: каждый ученик — это пользователь системы
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Классный руководитель ученика (учитель-куратор)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curator")
    private User curator;

    @Column(nullable = false)
    private Integer age;

    // Группа/класс ученика
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupEntity group;

    // Аудит: кто удалил / кто создал
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_admin_user_id")
    private User deletedByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private User createdByAdminUser;

    private String email;  // дублируем email для быстрых запросов без JOIN с User
    private String phone;

    // Родители ученика — обратная сторона Many-to-Many (основная сторона в Parent.students)
    @ManyToMany(mappedBy = "students")
    private Set<Parent> parents = new HashSet<>();

    // Курсы, на которые записан ученик
    @ManyToMany(mappedBy = "students")
    private Set<Course> courses = new HashSet<>();

    // Все оценки ученика
    @OneToMany(mappedBy = "student")
    private Set<Mark> marks = new HashSet<>();
}
