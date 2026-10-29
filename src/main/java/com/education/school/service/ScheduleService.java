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
        validate(request, null);
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

    // Урок должен быть корректным и не пересекаться с другими уроками того же учителя или класса в этот день
    private void validate(ScheduleRequest r, Integer selfId) {
        if (!r.endTime().isAfter(r.startTime())) {
            throw new IllegalArgumentException("Урок должен заканчиваться позже, чем начинается");
        }
        if (r.lessonNumber() < 1 || r.lessonNumber() > 8) {
            throw new IllegalArgumentException("Номер урока должен быть от 1 до 8");
        }
        for (Schedule other : scheduleRepository.findByDate(r.date())) {
            if (other.getId().equals(selfId)) continue;
            boolean overlap = other.getStartTime() != null && other.getEndTime() != null
                    && other.getStartTime().isBefore(r.endTime()) && r.startTime().isBefore(other.getEndTime());
            if (other.getTeacher().getId().equals(r.teacherId()) && overlap) {
                throw new IllegalArgumentException("Учитель уже ведёт урок в это время ("
                        + other.getGroup().getName() + ", " + other.getStartTime() + "–" + other.getEndTime() + ")");
            }
            if (other.getGroup().getId().equals(r.groupId())
                    && (overlap || other.getLessonNumber().equals(r.lessonNumber()))) {
                throw new IllegalArgumentException("У класса уже есть урок в это время ("
                        + other.getDiscipline().getName() + ", урок " + other.getLessonNumber() + ")");
            }
        }
    }

    @Transactional
    public Schedule update(Integer id, ScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        validate(request, id);

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
    public List<ScheduleResponse> getMyFullSchedule() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        String role = user.getRole().getName();

        try {
            if (role.equals("STUDENT")) {
                return studentRepository.findByUserId(user.getId())
                        .map(student -> scheduleRepository.findByGroupId(student.getGroup().getId()).stream()
                                .map(ScheduleResponse::from)
                                .collect(Collectors.toList()))
                        .orElse(List.of());
            } else if (role.equals("TEACHER")) {
                return teacherRepository.findByUserId(user.getId())
                        .map(teacher -> scheduleRepository.findByTeacherId(teacher.getId()).stream()
                                .map(ScheduleResponse::from)
                                .collect(Collectors.toList()))
                        .orElse(List.of());
            }
        } catch (Exception e) {
            return List.of();
        }
        return scheduleRepository.findAll().stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
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
