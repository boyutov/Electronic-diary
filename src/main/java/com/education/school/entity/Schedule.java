package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

// Запись в расписании — один урок в конкретный день
@Setter
@Getter
@Entity
@Table(name = "schedule")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Предмет урока
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

    // Учитель который проводит урок
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // Группа для которой урок
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;    // 1=Понедельник, 2=Вторник, ..., 5=Пятница

    @Column(name = "lesson_number", nullable = false)
    private Integer lessonNumber; // номер урока в этот день: 1, 2, 3...

    private String classroom;     // номер кабинета

    private LocalDate date;       // конкретная дата урока

    @Column(name = "start_time")
    private LocalTime startTime;  // время начала: 08:00

    @Column(name = "end_time")
    private LocalTime endTime;    // время окончания: 08:45
}
