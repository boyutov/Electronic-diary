package com.education.school.repository;

import com.education.school.entity.GradingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GradingSettingsRepository extends JpaRepository<GradingSettings, Integer> {
    Optional<GradingSettings> findBySchoolName(String schoolName);
}
