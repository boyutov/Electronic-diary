package com.education.school.repository;

import com.education.school.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Integer> {

    @Query("SELECT e FROM Exam e JOIN e.groups g WHERE g.id = :groupId AND e.examDate >= :from ORDER BY e.examDate")
    List<Exam> findByGroupIdAndDateFrom(@Param("groupId") Integer groupId, @Param("from") LocalDate from);

    @Query("SELECT e FROM Exam e WHERE e.examDate >= :from ORDER BY e.examDate")
    List<Exam> findAllFromDate(@Param("from") LocalDate from);

    @Query("SELECT e FROM Exam e JOIN e.createdByUser u WHERE u.id = :userId ORDER BY e.examDate")
    List<Exam> findByCreatedByUserId(@Param("userId") Long userId);
}
