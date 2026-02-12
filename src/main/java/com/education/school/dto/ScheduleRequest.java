package com.education.school.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScheduleRequest(
    @NotNull(message = "Discipline is required")
    Integer disciplineId,

    @NotNull(message = "Teacher is required")
    Integer teacherId,

    @NotNull(message = "Group is required")
    Integer groupId,

    @NotNull(message = "Day of week is required")
    @Min(value = 1, message = "Day of week must be between 1 and 7")
    @Max(value = 7, message = "Day of week must be between 1 and 7")
    Integer dayOfWeek,

    @NotNull(message = "Lesson number is required")
    @Min(value = 1, message = "Lesson number must be between 1 and 8")
    @Max(value = 8, message = "Lesson number must be between 1 and 8")
    Integer lessonNumber,

    String classroom
) {}
