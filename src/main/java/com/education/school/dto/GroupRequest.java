package com.education.school.dto;

import jakarta.validation.constraints.NotBlank;

public record GroupRequest(
    @NotBlank(message = "Group name is required")
    String name,

    Boolean hasOffice,
    String office,
    Integer course
) {}
