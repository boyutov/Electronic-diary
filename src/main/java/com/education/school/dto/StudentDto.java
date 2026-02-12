package com.education.school.dto;

import com.education.school.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class StudentDto {
    private Long id;
    private String firstName;
    private String secondName;
    private String thirdName;
    private String email;
    private Integer age;
    private List<MarkDto> marks;

    public static StudentDto from(Student student) {
        List<MarkDto> marks = student.getMarks().stream()
                .map(MarkDto::from)
                .collect(Collectors.toList());

        return new StudentDto(
            student.getId(),
            student.getUser().getFirstName(),
            student.getUser().getSecondName(),
            student.getUser().getThirdName(),
            student.getUser().getEmail(),
            student.getAge(),
            marks
        );
    }
}
