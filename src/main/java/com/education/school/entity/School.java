package com.education.school.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "access_password_hash")
    private String accessPasswordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_user_id")
    private User directorUser;

    @ManyToMany(mappedBy = "schools")
    private Set<User> users = new HashSet<>();
}
