package com.education.school.dto;

import com.education.school.entity.Mark;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
public class MarkDto {
    private Integer id;
    private Integer value;
    private OffsetDateTime createdAt;
    private String disciplineName;
    private Integer disciplineId;
    private String comment;
    private Long studentId;
    private String studentName;

    public static MarkDto from(Mark mark) {
        return new MarkDto(
            mark.getId(),
            mark.getValue(),
            mark.getCreatedAt(),
            mark.getDiscipline().getName(),
            mark.getDiscipline().getId(),
            mark.getComment(),
            mark.getStudent().getId(),
            mark.getStudent().getUser().getSecondName() + " " + mark.getStudent().getUser().getFirstName()
        );
    }
}
