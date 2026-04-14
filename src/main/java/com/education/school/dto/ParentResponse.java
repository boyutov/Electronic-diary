package com.education.school.dto;

import com.education.school.entity.Parent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class ParentResponse {
    private Integer id;
    private Long userId;
    private String firstName;
    private String secondName;
    private String thirdName;
    private String email;
    private String phone;
    private List<ChildDto> children;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ChildDto {
        private Long id;
        private String firstName;
        private String secondName;
        private String groupName;
    }

    public static ParentResponse from(Parent parent) {
        List<ChildDto> children = parent.getStudents().stream()
                .map(s -> new ChildDto(
                        s.getId(),
                        s.getUser().getFirstName(),
                        s.getUser().getSecondName(),
                        s.getGroup() != null ? s.getGroup().getName() : null
                ))
                .collect(Collectors.toList());

        return new ParentResponse(
            parent.getId(),
            parent.getUser().getId(),
            parent.getUser().getFirstName(),
            parent.getUser().getSecondName(),
            parent.getUser().getThirdName(),
            parent.getUser().getEmail(),
            parent.getPhone(),
            children
        );
    }
}
