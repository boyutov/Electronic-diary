package com.education.school.dto;

import com.education.school.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AdminResponse {
    private Long id;
    private String firstName;
    private String secondName;
    private String thirdName;
    private String email;

    public static AdminResponse from(User user) {
        return new AdminResponse(
            user.getId(),
            user.getFirstName(),
            user.getSecondName(),
            user.getThirdName(),
            user.getEmail()
        );
    }
}
