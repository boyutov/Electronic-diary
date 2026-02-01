package com.education.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SchoolRegistrationRequest(
        @NotBlank String schoolPassword,
        @NotBlank String firstName,
        @NotBlank String secondName,
        String thirdName,
        @Email @NotBlank String email
) {
}
