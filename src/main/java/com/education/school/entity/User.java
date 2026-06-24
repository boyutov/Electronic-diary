package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

// Реализует UserDetails — Spring Security работает с этим интерфейсом напрямую
@Setter
@Getter
@Entity                    // JPA-сущность — маппится на таблицу в БД
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // автоинкремент PostgreSQL (SERIAL)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;   // имя

    @Column(name = "second_name", nullable = false)
    private String secondName;  // фамилия

    @Column(name = "third_name")
    private String thirdName;   // отчество (необязательно)

    @Column(nullable = false)
    private String password;    // хэш BCrypt — НИКОГДА не хранится в открытом виде

    @Column(nullable = false)
    private String email;       // используется как логин (username)

    // Роль: ADMIN, DIRECTOR, TEACHER, STUDENT, PARENT
    // EAGER — роль загружается сразу с пользователем (нужна при каждом запросе)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Аудит: кто создал пользователя
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private User createdByAdminUser;

    // Аудит: кто удалил пользователя
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_admin_user_id")
    private User deletedByAdminUser;

    // Many-to-Many со школами: один пользователь может принадлежать нескольким школам
    // user_schools — промежуточная таблица с колонками user_id и school_id
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_schools",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "school_id")
    )
    private Set<School> schools = new HashSet<>();

    // Spring Security вызывает этот метод чтобы узнать права пользователя
    // Возвращаем роль + отдельные permissions (если есть)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        // Роль: "ADMIN", "TEACHER" и т.д.
        Stream<GrantedAuthority> roleAuthority = Stream.of(new SimpleGrantedAuthority(role.getAuthority()));
        // Дополнительные права из таблицы permissions (если настроены)
        Stream<GrantedAuthority> permissionAuthorities = role.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName()));
        return Stream.concat(roleAuthority, permissionAuthorities)
                .collect(Collectors.toUnmodifiableSet());
    }

    // Spring Security использует email как username (логин)
    @Override
    public String getUsername() {
        return email;
    }
}
