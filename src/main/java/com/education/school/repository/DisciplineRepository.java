package com.education.school.repository;

import com.education.school.entity.Discipline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplineRepository extends JpaRepository<Discipline, Integer> {
}
