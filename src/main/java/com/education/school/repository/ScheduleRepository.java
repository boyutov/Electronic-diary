package com.education.school.repository;

import com.education.school.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
    
    @Query("SELECT s FROM Schedule s WHERE s.group.id = :groupId AND s.date BETWEEN :startDate AND :endDate")
    List<Schedule> findByGroupIdAndDateBetween(@Param("groupId") Integer groupId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE s.teacher.id = :teacherId AND s.date BETWEEN :startDate AND :endDate")
    List<Schedule> findByTeacherIdAndDateBetween(@Param("teacherId") Integer teacherId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
