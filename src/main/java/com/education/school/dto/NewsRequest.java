package com.education.school.dto;

import jakarta.validation.constraints.NotBlank;

public record NewsRequest(
    @NotBlank(message = "Title is required")
    String title,

    @NotBlank(message = "Text is required")
    String text,

    Integer teacherId
) {}
