package com.education.school.dto;

import com.education.school.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class CourseResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer teacherId;
    private String teacherName;
    private int studentCount;
    private boolean enrolled; // для текущего студента
    private List<StudentInfo> students; // для учителя

    @Getter
    @Setter
    @AllArgsConstructor
    public static class StudentInfo {
        private Long id;
        private String name;
        private String email;
    }

    public static CourseResponse from(Course course) {
        return from(course, false, false);
    }

    public static CourseResponse from(Course course, boolean enrolled, boolean includeStudents) {
        String teacherName = null;
        Integer teacherId = null;
        if (course.getTeacher() != null) {
            teacherId = course.getTeacher().getId();
            teacherName = course.getTeacher().getUser().getSecondName()
                    + " " + course.getTeacher().getUser().getFirstName();
        }
        List<StudentInfo> students = includeStudents
                ? course.getStudents().stream()
                    .map(s -> new StudentInfo(
                        s.getId(),
                        s.getUser().getSecondName() + " " + s.getUser().getFirstName(),
                        s.getUser().getEmail()
                    )).collect(Collectors.toList())
                : List.of();

        return new CourseResponse(
            course.getId(),
            course.getName(),
            course.getDescription(),
            teacherId,
            teacherName,
            course.getStudents().size(),
            enrolled,
            students
        );
    }
}
