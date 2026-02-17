package com.education.school.dto;

import com.education.school.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String firstName;
    private String secondName;
    private String thirdName;
    private String email;
    private String role;

    public static UserProfileDto from(User user) {
        return new UserProfileDto(
            user.getId(),
            user.getFirstName(),
            user.getSecondName(),
            user.getThirdName(),
            user.getEmail(),
            user.getRole().getName()
        );
    }
}
