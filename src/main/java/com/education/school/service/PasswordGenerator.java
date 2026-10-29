package com.education.school.service;

import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {

    // Без похожих символов (0/O, 1/l/I), чтобы пароль было легко переписать из письма
    private static final String TEMPORARY_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    private final SecureRandom secureRandom = new SecureRandom();

    // Временный пароль вида "k7mxp-2qhta": выдаётся новому пользователю и меняется при первом входе
    public String generateTemporary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i == 5) sb.append('-');
            sb.append(TEMPORARY_ALPHABET.charAt(secureRandom.nextInt(TEMPORARY_ALPHABET.length())));
        }
        return sb.toString();
    }

    public String generate() {
        return UUID.randomUUID().toString();
    }

    public String generate(int length) {
        // Return a substring of a UUID to somewhat respect the length parameter,
        // though it's not guaranteed to be the exact length.
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, Math.min(length, uuid.length()));
    }
}
