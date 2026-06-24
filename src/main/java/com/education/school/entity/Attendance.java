package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// Посещаемость урока — фиксирует присутствие/отсутствие ученика на конкретном занятии
@Setter
@Getter
@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Ученик
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    // Предмет
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discipline_id")
    private Discipline discipline;

    // Конкретный урок из расписания
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;  // дата урока

    // Статус: "PRESENT" (присутствовал), "ABSENT" (отсутствовал), "LATE" (опоздал)
    private String status;

    @Column(name = "late_for_in_minutes")
    private Integer lateForInMinutes;  // на сколько минут опоздал

    @Column(columnDefinition = "TEXT")
    private String comment;  // причина отсутствия или комментарий учителя
}
