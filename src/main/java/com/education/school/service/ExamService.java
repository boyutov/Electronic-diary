package com.education.school.service;

import com.education.school.dto.ExamDto;
import com.education.school.entity.*;
import com.education.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final DisciplineRepository disciplineRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;

    /** Все экзамены (для админа/директора) */
    @Transactional(readOnly = true)
    public List<ExamDto> findAll() {
        return examRepository.findAllFromDate(LocalDate.now().minusMonths(1))
                .stream().map(ExamDto::from).collect(Collectors.toList());
    }

    /** Экзамены для текущего пользователя по его группе */
    @Transactional(readOnly = true)
    public List<ExamDto> findForCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        String role = user.getRole().getName();

        if (List.of("ADMIN", "DIRECTOR").contains(role)) {
            return findAll();
        }

        if ("TEACHER".equals(role)) {
            return examRepository.findByCreatedByUserId(user.getId())
                    .stream().map(ExamDto::from).collect(Collectors.toList());
        }

        if ("STUDENT".equals(role)) {
            return studentRepository.findByUserId(user.getId())
                    .filter(s -> s.getGroup() != null)
                    .map(s -> examRepository.findByGroupIdAndDateFrom(s.getGroup().getId(), LocalDate.now().minusDays(14))
                            .stream().map(ExamDto::from).collect(Collectors.toList()))
                    .orElse(List.of());
        }

        if ("PARENT".equals(role)) {
            return parentRepository.findByUserEmail(email)
                    .map(parent -> parent.getStudents().stream()
                            .filter(s -> s.getGroup() != null)
                            .flatMap(s -> examRepository
                                    .findByGroupIdAndDateFrom(s.getGroup().getId(), LocalDate.now().minusDays(14)).stream())
                            .distinct()
                            .map(ExamDto::from)
                            .collect(Collectors.toList()))
                    .orElse(List.of());
        }

        return List.of();
    }

    @Transactional
    public ExamDto create(Map<String, Object> req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);

        Exam exam = new Exam();
        exam.setTitle((String) req.get("title"));
        exam.setType(req.get("type") != null ? (String) req.get("type") : "EXAM");
        exam.setDescription((String) req.get("description"));
        exam.setExamDate(LocalDate.parse((String) req.get("examDate")));
        if (req.get("examTime") != null && !((String) req.get("examTime")).isBlank()) {
            exam.setExamTime(LocalTime.parse((String) req.get("examTime")));
        }
        exam.setCreatedByUser(user);
        exam.setCreatedAt(LocalDateTime.now());

        if (req.get("disciplineId") != null) {
            disciplineRepository.findById(((Number) req.get("disciplineId")).intValue())
                    .ifPresent(exam::setDiscipline);
        }

        @SuppressWarnings("unchecked")
        List<Integer> groupIds = (List<Integer>) req.get("groupIds");
        if (groupIds != null && !groupIds.isEmpty()) {
            exam.getGroups().addAll(groupRepository.findAllById(groupIds));
        }

        return ExamDto.from(examRepository.save(exam));
    }

    @Transactional
    public ExamDto update(Integer id, Map<String, Object> req) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        exam.setTitle((String) req.get("title"));
        exam.setType(req.get("type") != null ? (String) req.get("type") : exam.getType());
        exam.setDescription((String) req.get("description"));
        exam.setExamDate(LocalDate.parse((String) req.get("examDate")));
        exam.setExamTime(req.get("examTime") != null && !((String) req.get("examTime")).isBlank()
                ? LocalTime.parse((String) req.get("examTime")) : null);

        if (req.get("disciplineId") != null) {
            disciplineRepository.findById(((Number) req.get("disciplineId")).intValue())
                    .ifPresent(exam::setDiscipline);
        }

        @SuppressWarnings("unchecked")
        List<Integer> groupIds = (List<Integer>) req.get("groupIds");
        if (groupIds != null) {
            exam.getGroups().clear();
            exam.getGroups().addAll(groupRepository.findAllById(groupIds));
        }

        return ExamDto.from(examRepository.save(exam));
    }

    @Transactional
    public void delete(Integer id) {
        examRepository.deleteById(id);
    }
}
