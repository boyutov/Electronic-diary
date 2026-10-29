package com.education.school.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

// Учебный предмет: Математика, Физика, История и т.д.
@Setter
@Getter
@Entity
@Table(name = "disciplines")
public class Discipline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Школа-владелец записи (Multi-Tenancy): Hibernate сам подставляет текущую школу при сохранении
    // и добавляет "school_id = :текущая школа" во все запросы, включая findById
    @TenantId
    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    @Column(nullable = false)
    private String name;  // название предмета

    // Все учителя которые преподают этот предмет (обратная сторона Many-to-Many)
    @ManyToMany(mappedBy = "disciplines")
    private Set<Teacher> teachers = new HashSet<>();
}
