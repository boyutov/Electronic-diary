package com.education.school.dto;

import com.education.school.entity.Discipline;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DisciplineDto {
    private Integer id;
    private String name;

    public static DisciplineDto from(Discipline discipline) {
        return new DisciplineDto(
            discipline.getId(),
            discipline.getName()
        );
    }
}
