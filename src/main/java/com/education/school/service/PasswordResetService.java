package com.education.school.service;

import com.education.school.entity.PasswordResetToken;
import com.education.school.entity.School;
import com.education.school.entity.User;
import com.education.school.repository.PasswordResetTokenRepository;
import com.education.school.repository.SchoolRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

// "Забыли пароль?": на почту приходит одноразовая ссылка, пароль меняется только по ней.
// Сброс пароля без ссылки позволил бы любому, кто знает логин, сломать чужой вход
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_TTL_MINUTES = 60;
    private static final int RESEND_PAUSE_MINUTES = 5;

    private final PasswordResetTokenRepository tokenRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final CredentialMailService credentialMailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    // Ничего не сообщаем о том, существует ли аккаунт — иначе по форме можно перебирать логины
    @Transactional
    public void requestReset(String schoolName, String login) {
        credentialMailService.requireConfigured();
        if (schoolName == null || login == null) return;
        School school = schoolRepository.findByNameIgnoreCase(schoolName.trim()).orElse(null);
        User user = userRepository.findByEmail(login.trim());
        if (school == null || user == null || !userRepository.existsByIdAndSchoolsId(user.getId(), school.getId())) {
            return;
        }
        // Не чаще одного письма в 5 минут — защита от спама на чужую почту
        if (tokenRepository.existsByUserIdAndCreatedAtAfter(user.getId(), OffsetDateTime.now().minusMinutes(RESEND_PAUSE_MINUTES))) {
            return;
        }

        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        PasswordResetToken reset = new PasswordResetToken();
        reset.setUser(user);
        reset.setSchoolId(school.getId());
        reset.setTokenHash(sha256(token));
        reset.setCreatedAt(OffsetDateTime.now());
        reset.setExpiresAt(OffsetDateTime.now().plusMinutes(TOKEN_TTL_MINUTES));
        tokenRepository.save(reset);

        credentialMailService.sendPasswordResetLink(user, school.getName(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken reset = tokenRepository.findByTokenHash(sha256(token == null ? "" : token))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Ссылка недействительна или устарела — запросите новую"));

        User user = reset.getUser();
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Пароль должен быть не короче 8 символов");
        }
        if (newPassword.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Пароль не должен совпадать с логином");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false); // пароль задал сам владелец почты
        userRepository.save(user);
        reset.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(reset);
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
