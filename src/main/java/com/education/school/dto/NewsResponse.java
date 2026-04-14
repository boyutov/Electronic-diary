package com.education.school.dto;

import com.education.school.entity.News;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class NewsResponse {
    private Integer id;
    private String title;
    private String text;
    private LocalDateTime createdAt;
    private Integer teacherId;
    private String teacherName;

    public static NewsResponse from(News news) {
        Integer teacherId = null;
        String teacherName = null;
        if (news.getTeacher() != null) {
            teacherId = news.getTeacher().getId();
            teacherName = news.getTeacher().getUser().getFirstName() + " " + news.getTeacher().getUser().getSecondName();
        }
        return new NewsResponse(
            news.getId(),
            news.getTitle(),
            news.getText(),
            news.getCreatedAt(),
            teacherId,
            teacherName
        );
    }
}
