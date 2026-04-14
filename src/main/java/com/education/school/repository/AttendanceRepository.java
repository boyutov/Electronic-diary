package com.education.school.repository;

import com.education.school.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    List<Attendance> findByScheduleId(Integer scheduleId);

    Optional<Attendance> findByStudentIdAndScheduleId(Long studentId, Integer scheduleId);

    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.lessonDate BETWEEN :from AND :to")
    List<Attendance> findByStudentIdAndDateBetween(@Param("studentId") Long studentId,
                                                    @Param("from") LocalDate from,
                                                    @Param("to") LocalDate to);

    @Query("SELECT a FROM Attendance a WHERE a.student.group.id = :groupId AND a.lessonDate BETWEEN :from AND :to")
    List<Attendance> findByGroupIdAndDateBetween(@Param("groupId") Integer groupId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    @Query("SELECT a FROM Attendance a WHERE a.lessonDate BETWEEN :from AND :to")
    List<Attendance> findAllByDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
