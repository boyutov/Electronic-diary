package com.education.school.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MarkRequest(
    @NotNull(message = "Student ID is required")
    Long studentId,

    @NotNull(message = "Discipline ID is required")
    Integer disciplineId,

    @NotNull(message = "Mark value is required")
    @Min(value = 1, message = "Mark must be at least 1")
    @Max(value = 5, message = "Mark must be at most 5")
    Integer value,

    String comment
) {}
