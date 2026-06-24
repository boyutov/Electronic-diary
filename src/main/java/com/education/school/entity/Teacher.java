package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

// Учитель — расширяет пользователя профессиональными данными
@Setter
@Getter
@Entity
@Table(name = "teachers")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Связь с аккаунтом пользователя (для входа в систему)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;          // биография / описание

    @Column(name = "has_office")
    private Boolean hasOffice;   // есть ли отдельный кабинет

    private String office;       // номер кабинета

    @Column(name = "has_group")
    private Boolean hasGroup;    // является ли куратором класса

    // Группа, куратором которой является учитель
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupEntity group;

    private String phone;

    // Аудит
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_admin_user_id")
    private User deletedByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private User createdByAdminUser;

    // Предметы которые ведёт учитель — Many-to-Many через таблицу teacher_discipline
    @ManyToMany
    @JoinTable(
        name = "teacher_discipline",
        joinColumns = @JoinColumn(name = "teacher_id"),
        inverseJoinColumns = @JoinColumn(name = "discipline_id")
    )
    private Set<Discipline> disciplines = new HashSet<>();

    // Курсы которые ведёт учитель
    @OneToMany(mappedBy = "teacher")
    private Set<Course> courses = new HashSet<>();
}
