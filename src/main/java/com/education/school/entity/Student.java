package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curator", nullable = false)
    private User curator;

    @Column(nullable = false)
    private Integer age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_admin_user_id")
    private User deletedByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private User createdByAdminUser;

    private String email;

    @ManyToMany(mappedBy = "students")
    private Set<Parent> parents = new HashSet<>();

    @ManyToMany(mappedBy = "students")
    private Set<Course> courses = new HashSet<>();

    @OneToMany(mappedBy = "student")
    private Set<Mark> marks = new HashSet<>();
}
