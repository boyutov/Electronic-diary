package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

// Оценка ученика по предмету
@Setter
@Getter
@Entity
@Table(name = "marks")
public class Mark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;  // дата выставления оценки

    // Мягкое удаление: вместо DELETE из БД — ставим дату удаления
    // Запросы фильтруют по deletedAt IS NULL чтобы не показывать удалённые
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // Ученик которому поставлена оценка
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Предмет по которому оценка
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

    @Column(nullable = false)
    private Integer value;   // значение: 2, 3, 4, 5

    // Учитель который поставил оценку
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "given_by_teacher_id", nullable = false)
    private Teacher givenByTeacher;

    @Column(columnDefinition = "TEXT")
    private String comment;  // комментарий учителя или причина изменения/удаления
}
