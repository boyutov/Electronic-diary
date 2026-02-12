package com.education.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PollRequest(
    @NotBlank(message = "Title is required")
    String title,

    String description,

    @NotEmpty(message = "At least one option is required")
    List<String> options,

    @NotNull(message = "Active status is required")
    Boolean active
) {}
