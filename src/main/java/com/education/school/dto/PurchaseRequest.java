package com.education.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PurchaseRequest(
        @NotBlank String schoolName,
        @Email @NotBlank String contactEmail,
        String contactPhone
) {
}
