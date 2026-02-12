package com.education.school.dto;

import com.education.school.entity.Teacher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TeacherResponse {
    private Integer id; // Teacher ID
    private Long userId;
    private String firstName;
    private String secondName;

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
            teacher.getId(),
            teacher.getUser().getId(),
            teacher.getUser().getFirstName(),
            teacher.getUser().getSecondName()
        );
    }
}
