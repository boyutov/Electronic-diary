package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Setter
@Getter
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "second_name", nullable = false)
    private String secondName;

    @Column(name = "third_name")
    private String thirdName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_user_id")
    private User createdByAdminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_admin_user_id")
    private User deletedByAdminUser;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        Stream<GrantedAuthority> roleAuthority = Stream.of(new SimpleGrantedAuthority(role.getAuthority()));
        Stream<GrantedAuthority> permissionAuthorities = role.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName()));
        return Stream.concat(roleAuthority, permissionAuthorities)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getUsername() {
        return email;
    }
}
