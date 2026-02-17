package com.education.school.service;

import com.education.school.dto.ScheduleRequest;
import com.education.school.dto.ScheduleResponse;
import com.education.school.entity.*;
import com.education.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DisciplineRepository disciplineRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAll() {
        return scheduleRepository.findAll().stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
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
        schedule.setLessonNumber(request.lessonNumber());
        schedule.setClassroom(request.classroom());
        schedule.setDate(request.date());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        
        schedule.setDayOfWeek(request.date().getDayOfWeek().getValue());

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public Schedule update(Integer id, ScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        Discipline discipline = disciplineRepository.findById(request.disciplineId())
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found"));
        Teacher teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        GroupEntity group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        schedule.setDiscipline(discipline);
        schedule.setTeacher(teacher);
        schedule.setGroup(group);
        schedule.setLessonNumber(request.lessonNumber());
        schedule.setClassroom(request.classroom());
        schedule.setDate(request.date());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setDayOfWeek(request.date().getDayOfWeek().getValue());

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void delete(Integer id) {
        scheduleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getMyScheduleForToday() {
        return getMyScheduleForPeriod(LocalDate.now(), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getMyScheduleForPeriod(LocalDate startDate, LocalDate endDate) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);

        if (user.getRole().getName().equals("STUDENT")) {
            Student student = studentRepository.findAll().stream()
                    .filter(s -> s.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Student profile not found"));
            
            return scheduleRepository.findByGroupIdAndDateBetween(student.getGroup().getId(), startDate, endDate).stream()
                    .map(ScheduleResponse::from)
                    .collect(Collectors.toList());
        } else if (user.getRole().getName().equals("TEACHER")) {
            Teacher teacher = teacherRepository.findAll().stream()
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Teacher profile not found"));

            return scheduleRepository.findByTeacherIdAndDateBetween(teacher.getId(), startDate, endDate).stream()
                    .map(ScheduleResponse::from)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getScheduleByGroupId(Integer groupId, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.findByGroupIdAndDateBetween(groupId, startDate, endDate).stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
}
