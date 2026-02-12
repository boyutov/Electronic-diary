package com.education.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseRequest(
    @NotBlank(message = "Course name is required")
    String name,

    @NotNull(message = "Teacher is required")
    Integer teacherId,

    String description
) {}
