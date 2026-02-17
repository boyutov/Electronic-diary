package com.education.school.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleRequest(
    @NotNull(message = "Discipline is required")
    Integer disciplineId,

    @NotNull(message = "Teacher is required")
    Integer teacherId,

    @NotNull(message = "Group is required")
    Integer groupId,

    @NotNull(message = "Lesson number is required")
    Integer lessonNumber,

    String classroom,

    @NotNull(message = "Date is required")
    LocalDate date,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    LocalTime endTime
) {}
