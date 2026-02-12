package com.education.school.service;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {

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
