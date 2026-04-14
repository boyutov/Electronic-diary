package com.education.school.dto;

import com.education.school.entity.Exam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class ExamDto {
    private Integer id;
    private String title;
    private String type;
    private String description;
    private LocalDate examDate;
    private LocalTime examTime;
    private Integer disciplineId;
    private String disciplineName;
    private Long createdByUserId;
    private String createdByName;
    private List<Integer> groupIds;
    private List<String> groupNames;

    public static ExamDto from(Exam e) {
        return new ExamDto(
            e.getId(),
            e.getTitle(),
            e.getType(),
            e.getDescription(),
            e.getExamDate(),
            e.getExamTime(),
            e.getDiscipline() != null ? e.getDiscipline().getId() : null,
            e.getDiscipline() != null ? e.getDiscipline().getName() : null,
            e.getCreatedByUser() != null ? e.getCreatedByUser().getId() : null,
            e.getCreatedByUser() != null
                ? e.getCreatedByUser().getSecondName() + " " + e.getCreatedByUser().getFirstName()
                : null,
            e.getGroups().stream().map(g -> g.getId()).collect(Collectors.toList()),
            e.getGroups().stream().map(g -> g.getName()).collect(Collectors.toList())
        );
    }
}
