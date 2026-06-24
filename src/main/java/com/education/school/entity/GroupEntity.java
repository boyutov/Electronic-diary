package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

// Учебная группа (класс): 9А, 10Б, 11В и т.д.
@Setter
@Getter
@Entity
@Table(name = "groups")
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;   // название: "9А", "10Б"

    // Куратор (классный руководитель) — учитель назначенный администратором
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curator")
    private User curator;

    @Column(name = "has_office")
    private Boolean hasOffice;   // есть ли закреплённый кабинет

    private String office;       // номер кабинета

    private Integer course;      // номер класса: 9, 10, 11

    // Тип финансирования: BUDGET (бюджет) или CONTRACT (платное)
    @Column(name = "funding_type")
    private String fundingType;

    // Аудит
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_admin_user_id")
    private User deletedByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private User createdByAdminUser;

    // Все ученики в этой группе
    @OneToMany(mappedBy = "group")
    private Set<Student> students = new HashSet<>();

    // Учителя, закреплённые за этой группой (кураторы)
    @OneToMany(mappedBy = "group")
    private Set<Teacher> teachers = new HashSet<>();
}
