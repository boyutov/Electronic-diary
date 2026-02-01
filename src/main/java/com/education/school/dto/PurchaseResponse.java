package com.education.school.dto;

public record PurchaseResponse(Long schoolId, Long adminUserId, String generatedPassword) {
}
