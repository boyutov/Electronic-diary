package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

// Родитель — расширяет пользователя, привязан к одному или нескольким ученикам
@Setter
@Getter
@Entity
@Table(name = "parents")
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Аккаунт пользователя для входа в систему
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String phone;

    // Дети этого родителя — Many-to-Many через таблицу parent_student
    // Один родитель может иметь несколько детей, один ребёнок может иметь нескольких родителей
    @ManyToMany
    @JoinTable(
        name = "parent_student",
        joinColumns = @JoinColumn(name = "parent_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<Student> students = new HashSet<>();
}
