package com.education.school.dto;

import com.education.school.entity.GroupEntity;
import com.education.school.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CuratorProfileDto {
    private Long userId;
    private String firstName;
    private String secondName;
    private Integer groupId;
    private String groupName;

    public static CuratorProfileDto from(User user, GroupEntity group) {
        return new CuratorProfileDto(
            user.getId(),
            user.getFirstName(),
            user.getSecondName(),
            group.getId(),
            group.getName()
        );
    }
}
