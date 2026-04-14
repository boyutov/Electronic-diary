package com.education.school.service;

import com.education.school.dto.AttendanceDto;
import com.education.school.entity.*;
import com.education.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;

    /** Посещаемость по конкретному уроку (scheduleId) */
    @Transactional(readOnly = true)
    public List<AttendanceDto> findBySchedule(Integer scheduleId) {
        return attendanceRepository.findByScheduleId(scheduleId)
                .stream().map(AttendanceDto::from).collect(Collectors.toList());
    }

    /** Сохранить посещаемость урока — upsert по student+schedule */
    @Transactional
    public List<AttendanceDto> saveForSchedule(Integer scheduleId, List<Map<String, Object>> records) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        return records.stream().map(rec -> {
            Long studentId = ((Number) rec.get("studentId")).longValue();
            String status = (String) rec.get("status");
            Integer lateMinutes = rec.get("lateForInMinutes") != null
                    ? ((Number) rec.get("lateForInMinutes")).intValue() : null;
            String comment = (String) rec.get("comment");

            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

            Attendance a = attendanceRepository
                    .findByStudentIdAndScheduleId(studentId, scheduleId)
                    .orElse(new Attendance());

            a.setStudent(student);
            a.setDiscipline(schedule.getDiscipline());
            a.setSchedule(schedule);
            a.setLessonDate(schedule.getDate() != null ? schedule.getDate() : LocalDate.now());
            a.setStatus(status);
            a.setLateForInMinutes("late".equals(status) ? lateMinutes : null);
            a.setComment(comment);

            return AttendanceDto.from(attendanceRepository.save(a));
        }).collect(Collectors.toList());
    }

    /** Посещаемость текущего студента за период */
    @Transactional(readOnly = true)
    public List<AttendanceDto> findMyAttendance(LocalDate from, LocalDate to) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        return studentRepository.findByUserId(user.getId())
                .map(s -> attendanceRepository.findByStudentIdAndDateBetween(s.getId(), from, to)
                        .stream().map(AttendanceDto::from).collect(Collectors.toList()))
                .orElse(List.of());
    }

    /** Посещаемость детей текущего родителя за период */
    @Transactional(readOnly = true)
    public List<AttendanceDto> findChildrenAttendance(LocalDate from, LocalDate to) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return parentRepository.findByUserEmail(email)
                .map(parent -> parent.getStudents().stream()
                        .flatMap(s -> attendanceRepository
                                .findByStudentIdAndDateBetween(s.getId(), from, to).stream())
                        .map(AttendanceDto::from)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Transactional
    public void delete(Integer id) {
        attendanceRepository.deleteById(id);
    }

    /** Аналитика посещаемости по всем группам за период (для админа/директора) */
    @Transactional(readOnly = true)
    public List<AttendanceDto> findAllByPeriod(LocalDate from, LocalDate to) {
        return attendanceRepository.findAllByDateBetween(from, to)
                .stream().map(AttendanceDto::from).collect(Collectors.toList());
    }

    /** Аналитика посещаемости конкретной группы за период */
    @Transactional(readOnly = true)
    public List<AttendanceDto> findByGroupAndPeriod(Integer groupId, LocalDate from, LocalDate to) {
        return attendanceRepository.findByGroupIdAndDateBetween(groupId, from, to)
                .stream().map(AttendanceDto::from).collect(Collectors.toList());
    }
}
