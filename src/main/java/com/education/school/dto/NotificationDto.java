package com.education.school.dto;

import com.education.school.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String type;
    private String title;
    private String body;
    private Boolean isRead;
    private OffsetDateTime createdAt;
    private String link;

    public static NotificationDto from(Notification n) {
        return new NotificationDto(
            n.getId(), n.getType(), n.getTitle(), n.getBody(),
            n.getIsRead(), n.getCreatedAt(), n.getLink()
        );
    }
}
