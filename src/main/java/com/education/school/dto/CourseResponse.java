package com.education.school.dto;

import com.education.school.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CourseResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer teacherId;
    private String teacherName;

    public static CourseResponse from(Course course) {
        String teacherName = null;
        Integer teacherId = null;
        if (course.getTeacher() != null) {
            teacherId = course.getTeacher().getId();
            teacherName = course.getTeacher().getUser().getFirstName()
                    + " " + course.getTeacher().getUser().getSecondName();
        }
        return new CourseResponse(
            course.getId(),
            course.getName(),
            course.getDescription(),
            teacherId,
            teacherName
        );
    }
}
