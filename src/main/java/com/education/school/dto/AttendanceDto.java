package com.education.school.dto;

import com.education.school.entity.Attendance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class AttendanceDto {
    private Integer id;
    private Long studentId;
    private String studentName;
    private String groupName;
    private Integer disciplineId;
    private String disciplineName;
    private Integer scheduleId;
    private Integer lessonNumber;
    private LocalDate lessonDate;
    private String status;
    private Integer lateForInMinutes;
    private String comment;

    public static AttendanceDto from(Attendance a) {
        String groupName = null;
        if (a.getStudent() != null && a.getStudent().getGroup() != null) {
            groupName = a.getStudent().getGroup().getName();
        }
        return new AttendanceDto(
            a.getId(),
            a.getStudent() != null ? a.getStudent().getId() : null,
            a.getStudent() != null
                ? a.getStudent().getUser().getSecondName() + " " + a.getStudent().getUser().getFirstName()
                : null,
            groupName,
            a.getDiscipline() != null ? a.getDiscipline().getId() : null,
            a.getDiscipline() != null ? a.getDiscipline().getName() : null,
            a.getSchedule() != null ? a.getSchedule().getId() : null,
            a.getSchedule() != null ? a.getSchedule().getLessonNumber() : null,
            a.getLessonDate(),
            a.getStatus(),
            a.getLateForInMinutes(),
            a.getComment()
        );
    }
}
