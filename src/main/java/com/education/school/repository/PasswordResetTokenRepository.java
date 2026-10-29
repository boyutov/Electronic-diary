package com.education.school.repository;

import com.education.school.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // Защита от спама письмами: не чаще одной ссылки в несколько минут
    boolean existsByUserIdAndCreatedAtAfter(Long userId, OffsetDateTime after);
}
