package com.education.school.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

// Дополнительный курс — факультатив или кружок, который ведёт учитель
// Ученики записываются добровольно через Many-to-Many
@Setter
@Getter
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Школа-владелец записи (Multi-Tenancy): Hibernate сам подставляет текущую школу при сохранении
    // и добавляет "school_id = :текущая школа" во все запросы, включая findById
    @TenantId
    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    @Column(nullable = false)
    private String name;  // название курса

    // Учитель который ведёт этот курс
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(columnDefinition = "TEXT")
    private String description;  // описание курса

    // Ученики записанные на курс — Many-to-Many через таблицу student_course
    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "course_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<Student> students = new HashSet<>();
}
