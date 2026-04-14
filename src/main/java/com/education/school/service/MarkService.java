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

    @Transactional
    public MarkDto create(MarkRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email);

        Teacher teacher = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Only teachers can give marks"));

        Discipline discipline = disciplineRepository.findById(request.disciplineId())
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found"));

        // Check if teacher is allowed to teach this discipline
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
        mark.setCreatedAt(OffsetDateTime.now());

        return MarkDto.from(markRepository.save(mark));
    }
}
