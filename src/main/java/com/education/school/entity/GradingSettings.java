package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Настройки системы оценивания для конкретной школы
// Каждая школа может настроить свою систему: четверти, семестры или год
@Setter
@Getter
@Entity
@Table(name = "grading_settings")
public class GradingSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Один к одному со школой — у каждой школы свои настройки оценивания
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    // Тип периода: QUARTER (четверти), SEMESTER (семестры), YEAR (годовые)
    @Column(name = "period_type", nullable = false)
    private String periodType = "QUARTER";

    // Месяц начала учебного года: 9 = сентябрь
    @Column(name = "academic_year_start", nullable = false)
    private Integer academicYearStart = 9;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
