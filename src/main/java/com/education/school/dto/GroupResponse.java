package com.education.school.dto;

import com.education.school.entity.GroupEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GroupResponse {
    private Integer id;
    private String name;

    public static GroupResponse from(GroupEntity group) {
        return new GroupResponse(
            group.getId(),
            group.getName()
        );
    }
}
