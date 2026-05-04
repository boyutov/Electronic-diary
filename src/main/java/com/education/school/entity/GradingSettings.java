package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "grading_settings")
public class GradingSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    @Column(name = "period_type", nullable = false)
    private String periodType = "QUARTER"; // QUARTER, SEMESTER, YEAR

    @Column(name = "academic_year_start", nullable = false)
    private Integer academicYearStart = 9; // сентябрь

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
