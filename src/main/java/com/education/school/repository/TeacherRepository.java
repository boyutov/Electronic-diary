package com.education.school.repository;

import com.education.school.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
    Optional<Teacher> findByUserId(Long userId);
}
