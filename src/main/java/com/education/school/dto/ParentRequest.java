package com.education.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParentRequest(
    @NotBlank(message = "First name is required")
    String firstName,

    @NotBlank(message = "Second name is required")
    String secondName,

    String thirdName,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @Size(min = 6, message = "Password must be at least 6 characters long")
    String password,

    String phone
) {}
