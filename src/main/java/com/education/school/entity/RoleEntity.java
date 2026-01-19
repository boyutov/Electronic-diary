package com.education.school.entity;

import org.springframework.security.core.GrantedAuthority;

public enum RoleEntity implements GrantedAuthority {
    ADMIN("admin");

    private final String title;

    RoleEntity(String title) {
        this.title = title;
    }

    @Override
     public String getAuthority() {
        return title;
    }
}
