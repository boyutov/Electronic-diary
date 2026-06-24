package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Дополнительное право доступа привязанное к роли
// Позволяет гранулярно настраивать разрешения внутри роли
@Setter
@Getter
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;  // название права: например "READ_MARKS", "EDIT_SCHEDULE"

    // К какой роли привязано это право
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
