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
    private Boolean hasOffice;
    private String office;
    private Integer course;
    private String fundingType;
    private String curatorName;

    public static GroupResponse from(GroupEntity group) {
        String curatorName = null;
        if (group.getCurator() != null) {
            curatorName = group.getCurator().getSecondName() + " " + group.getCurator().getFirstName();
        }
        return new GroupResponse(
            group.getId(),
            group.getName(),
            group.getHasOffice(),
            group.getOffice(),
            group.getCourse(),
            group.getFundingType(),
            curatorName
        );
    }
}
