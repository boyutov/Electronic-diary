package com.education.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PurchaseRequest(
    @NotBlank(message = "School name is required")
    String schoolName,

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    String contactEmail,

    String contactPhone,

    @NotNull(message = "Student count is required")
    @Min(value = 1, message = "At least 1 student required")
    Integer studentCount,

    @NotNull(message = "Duration is required")
    Integer durationMonths,

    String promoCode
) {}
