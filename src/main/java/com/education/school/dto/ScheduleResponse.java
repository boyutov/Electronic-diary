package com.education.school.dto;

import com.education.school.entity.Schedule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
public class ScheduleResponse {
    private Integer id;
    private Integer disciplineId;
    private String disciplineName;
    private Integer teacherId;
    private String teacherName;
    private Integer groupId;
    private String groupName;
    private Integer lessonNumber;
    private String classroom;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
            schedule.getId(),
            schedule.getDiscipline().getId(),
            schedule.getDiscipline().getName(),
            schedule.getTeacher().getId(),
            schedule.getTeacher().getUser().getFirstName() + " " + schedule.getTeacher().getUser().getSecondName(),
            schedule.getGroup().getId(),
            schedule.getGroup().getName(),
            schedule.getLessonNumber(),
            schedule.getClassroom(),
            schedule.getDate(),
            schedule.getStartTime(),
            schedule.getEndTime()
        );
    }
}
