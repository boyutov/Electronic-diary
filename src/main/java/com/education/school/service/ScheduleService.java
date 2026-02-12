package com.education.school.service;

import com.education.school.dto.ScheduleRequest;
import com.education.school.entity.Discipline;
import com.education.school.entity.GroupEntity;
import com.education.school.entity.Schedule;
import com.education.school.entity.Teacher;
import com.education.school.repository.DisciplineRepository;
import com.education.school.repository.GroupRepository;
import com.education.school.repository.ScheduleRepository;
import com.education.school.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DisciplineRepository disciplineRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;

    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
    }

    @Transactional
    public Schedule create(ScheduleRequest request) {
        Discipline discipline = disciplineRepository.findById(request.disciplineId())
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found"));
        Teacher teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        GroupEntity group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        Schedule schedule = new Schedule();
        schedule.setDiscipline(discipline);
        schedule.setTeacher(teacher);
        schedule.setGroup(group);
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setLessonNumber(request.lessonNumber());
        schedule.setClassroom(request.classroom());

        return scheduleRepository.save(schedule);
    }
}
