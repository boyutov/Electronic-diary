package com.education.school.repository;

import com.education.school.entity.Mark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MarkRepository extends JpaRepository<Mark, Integer> {

    @Query("SELECT m FROM Mark m WHERE m.student.group.id = :groupId AND m.discipline.id = :disciplineId AND m.deletedAt IS NULL ORDER BY m.student.user.secondName, m.createdAt")
    List<Mark> findByGroupAndDiscipline(@Param("groupId") Integer groupId, @Param("disciplineId") Integer disciplineId);

    @Query("SELECT m FROM Mark m WHERE m.givenByTeacher.id = :teacherId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    List<Mark> findByTeacherId(@Param("teacherId") Integer teacherId);
}
