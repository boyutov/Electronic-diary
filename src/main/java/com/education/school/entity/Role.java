package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashSet;
import java.util.Set;

// Роль пользователя: ADMIN, DIRECTOR, TEACHER, STUDENT, PARENT, MINISTRY
// Реализует GrantedAuthority — Spring Security использует это для проверки прав доступа
@Setter
@Getter
@Entity
@Table(name = "roles")
public class Role implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;  // "ADMIN", "TEACHER" и т.д.

    // Все пользователи с этой ролью
    @OneToMany(mappedBy = "role")
    private Set<User> users = new HashSet<>();

    // Дополнительные права доступа для этой роли
    // EAGER — загружаем сразу, нужны при каждой проверке авторизации
    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER)
    private Set<Permission> permissions = new HashSet<>();

    // Spring Security вызывает getAuthority() чтобы получить строковое имя роли
    @Override
    public String getAuthority() {
        return name;
    }
}
