package com.education.school.dto;

import com.education.school.entity.Discipline;
import com.education.school.entity.Teacher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class TeacherResponse {
    private Integer id;
    private Long userId;
    private String firstName;
    private String secondName;
    private String thirdName;
    private String email;
    private String phone;
    private String bio;
    private Boolean hasOffice;
    private String office;
    private Boolean hasGroup;
    private Integer groupId;
    private String groupName;
    private List<Integer> disciplineIds;

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
            teacher.getId(),
            teacher.getUser().getId(),
            teacher.getUser().getFirstName(),
            teacher.getUser().getSecondName(),
            teacher.getUser().getThirdName(),
            teacher.getUser().getEmail(),
            teacher.getPhone(),
            teacher.getBio(),
            teacher.getHasOffice(),
            teacher.getOffice(),
            teacher.getHasGroup(),
            teacher.getGroup() != null ? teacher.getGroup().getId() : null,
            teacher.getGroup() != null ? teacher.getGroup().getName() : null,
            teacher.getDisciplines().stream().map(Discipline::getId).collect(Collectors.toList())
        );
    }
}
