package com.education.school.repository;

import com.education.school.entity.Mark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkRepository extends JpaRepository<Mark, Integer> {
}
