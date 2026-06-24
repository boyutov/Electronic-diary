package com.education.school.service;

import com.education.school.dto.StudentDto;
import com.education.school.dto.StudentRequest;
import com.education.school.entity.GroupEntity;
import com.education.school.entity.Role;
import com.education.school.entity.Student;
import com.education.school.entity.User;
import com.education.school.repository.GroupRepository;
import com.education.school.repository.RoleRepository;
import com.education.school.repository.StudentRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Сервис управления учениками
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    // Получить профиль текущего ученика — читаем email из JWT-контекста
    @Transactional(readOnly = true)
    public StudentDto getCurrentStudent() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Student not found"));

        return StudentDto.from(student);
    }

    // Создать нового ученика
    @Transactional
    public Student create(StudentRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required for new student");
        }

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("Role STUDENT not found"));

        // Определяем кто создаёт — для проверки прав и наследования школы
        String creatorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepository.findByEmail(creatorEmail);

        GroupEntity group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Учитель-куратор может создавать учеников только в своей группе
        if ("TEACHER".equals(creator.getRole().getName())) {
            Optional<GroupEntity> curatorGroup = groupRepository.findByCuratorId(creator.getId());
            if (curatorGroup.isEmpty() || !curatorGroup.get().getId().equals(group.getId())) {
                throw new SecurityException("You can only create students in your own group.");
            }
        }

        // Создаём User (аккаунт для входа)
        User user = new User();
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(studentRole);

        // Ученик наследует школу создателя
        if (creator != null) {
            user.getSchools().addAll(creator.getSchools());
        }

        user = userRepository.save(user);

        // Создаём Student (профиль ученика)
        Student student = new Student();
        student.setUser(user);
        student.setAge(request.age());
        student.setGroup(group);
        student.setCurator(group.getCurator()); // куратор берётся из группы
        student.setEmail(request.email());

        return studentRepository.save(student);
    }

    // Обновить данные ученика
    @Transactional
    public Student update(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        User user = student.getUser();
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        user.setEmail(request.email());

        // Меняем пароль только если передан
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        userRepository.save(user);

        GroupEntity group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        student.setAge(request.age());
        student.setGroup(group);
        student.setCurator(group.getCurator());

        return studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public Student findById(Long id) {
        return studentRepository.findById(id).orElse(null);
    }

    public void deleteById(Long id) {
        studentRepository.deleteById(id);
    }
}
