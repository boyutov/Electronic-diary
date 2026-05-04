package com.education.school.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MarkRequest(
    @NotNull(message = "Student ID is required")
    Long studentId,

    @NotNull(message = "Discipline ID is required")
    Integer disciplineId,

    @NotNull(message = "Mark value is required")
    @Min(value = 1, message = "Mark must be at least 1")
    @Max(value = 100, message = "Mark must be at most 100")
    Integer value,

    String comment,

    // Дата оценки — если null, используется сегодня
    LocalDate markDate
) {}
