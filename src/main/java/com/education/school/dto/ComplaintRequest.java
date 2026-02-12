package com.education.school.dto;

import jakarta.validation.constraints.NotBlank;

public record ComplaintRequest(
    @NotBlank(message = "Content is required")
    String content,
    
    Boolean isAnonymous
) {}
