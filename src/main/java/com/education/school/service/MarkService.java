package com.education.school.service;

import com.education.school.dto.MarkDto;
import com.education.school.dto.MarkRequest;
import com.education.school.entity.Discipline;
import com.education.school.entity.Mark;
import com.education.school.entity.Student;
import com.education.school.entity.Teacher;
import com.education.school.entity.User;
import com.education.school.repository.DisciplineRepository;
import com.education.school.repository.MarkRepository;
import com.education.school.repository.StudentRepository;
import com.education.school.repository.TeacherRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class MarkService {

    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final DisciplineRepository disciplineRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public MarkDto create(MarkRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email);

        Teacher teacher = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Only teachers can give marks"));

        Discipline discipline = disciplineRepository.findById(request.disciplineId())
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found"));

        if (!teacher.getDisciplines().contains(discipline)) {
            throw new SecurityException("You are not allowed to give marks for this discipline");
        }

        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        Mark mark = new Mark();
        mark.setStudent(student);
        mark.setDiscipline(discipline);
        mark.setValue(request.value());
        mark.setComment(request.comment());
        mark.setGivenByTeacher(teacher);
        mark.setCreatedAt(request.markDate() != null
                ? request.markDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC)
                : OffsetDateTime.now());

        MarkDto result = MarkDto.from(markRepository.save(mark));

        // Уведомление студенту
        String studentName = student.getUser().getFirstName();
        notificationService.send(
            student.getUser().getId(), "MARK",
            "Новая оценка: " + request.value() + " — " + discipline.getName(),
            "Учитель: " + currentUser.getSecondName() + " " + currentUser.getFirstName()
                + (request.comment() != null && !request.comment().isBlank() ? ". Комментарий: " + request.comment() : ""),
            "/student"
        );
        // Уведомление родителям
        student.getParents().forEach(parent ->
            notificationService.send(
                parent.getUser().getId(), "MARK",
                "Оценка у " + studentName + ": " + request.value() + " — " + discipline.getName(),
                "Учитель: " + currentUser.getSecondName() + " " + currentUser.getFirstName(),
                "/parent"
            )
        );

        return result;
    }

    /** Оценки учителя по группе и предмету */
    @Transactional(readOnly = true)
    public java.util.List<MarkDto> findByGroupAndDiscipline(Integer groupId, Integer disciplineId) {
        return markRepository.findByGroupAndDiscipline(groupId, disciplineId)
                .stream().map(MarkDto::from).collect(java.util.stream.Collectors.toList());
    }

    /** Изменить оценку — комментарий обязателен */
    @Transactional
    public MarkDto update(Integer id, Integer newValue, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Необходимо указать причину изменения");
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email);
        Teacher teacher = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Only teachers can edit marks"));

        Mark mark = markRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mark not found"));

        if (!mark.getGivenByTeacher().getId().equals(teacher.getId())) {
            throw new SecurityException("Можно редактировать только свои оценки");
        }

        int oldValue = mark.getValue();
        mark.setValue(newValue);
        mark.setComment("Изменено с " + oldValue + " на " + newValue + ". Причина: " + reason);
        MarkDto result = MarkDto.from(markRepository.save(mark));

        // Уведомление
        notificationService.send(
            mark.getStudent().getUser().getId(), "MARK",
            "Оценка изменена: " + oldValue + " → " + newValue + " — " + mark.getDiscipline().getName(),
            "Причина: " + reason, "/grades"
        );
        mark.getStudent().getParents().forEach(p ->
            notificationService.send(p.getUser().getId(), "MARK",
                "Оценка изменена у " + mark.getStudent().getUser().getFirstName() + ": " + oldValue + " → " + newValue,
                "Причина: " + reason, "/parent")
        );
        return result;
    }

    /** Удалить оценку — комментарий обязателен */
    @Transactional
    public void delete(Integer id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Необходимо указать причину удаления");
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email);
        Teacher teacher = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Only teachers can delete marks"));

        Mark mark = markRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mark not found"));

        if (!mark.getGivenByTeacher().getId().equals(teacher.getId())) {
            throw new SecurityException("Можно удалять только свои оценки");
        }

        int val = mark.getValue();
        String disc = mark.getDiscipline().getName();
        mark.setDeletedAt(OffsetDateTime.now());
        mark.setComment("Удалено. Причина: " + reason);
        markRepository.save(mark);

        notificationService.send(
            mark.getStudent().getUser().getId(), "MARK",
            "Оценка " + val + " удалена — " + disc,
            "Причина: " + reason, "/grades"
        );
        mark.getStudent().getParents().forEach(p ->
            notificationService.send(p.getUser().getId(), "MARK",
                "Оценка " + val + " удалена у " + mark.getStudent().getUser().getFirstName() + " — " + disc,
                "Причина: " + reason, "/parent")
        );
    }
}
