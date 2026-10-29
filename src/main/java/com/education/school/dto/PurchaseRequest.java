package com.education.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PurchaseRequest(
    @NotBlank(message = "School name is required")
    String schoolName,

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    String contactEmail,

    String contactPhone,

    // Регистрация бесплатная: поля оставлены для совместимости со старыми клиентами и не обязательны
    Integer studentCount,

    Integer durationMonths,

    String promoCode
) {}
