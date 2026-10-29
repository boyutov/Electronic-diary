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
    private final CurrentSchoolService currentSchoolService;
    private final CredentialMailService credentialMailService;

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
        return create(request, null);
    }

    // contactEmail — почта родителя для ребёнка без своей почты: туда уйдёт временный пароль
    @Transactional
    public Student create(StudentRequest request, String contactEmail) {
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("Role STUDENT not found"));

        // Определяем кто создаёт — для проверки прав и наследования школы
        String creatorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepository.findByEmail(creatorEmail);

        GroupEntity group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Учитель-куратор может создавать учеников только в своей группе
        checkTeacherOwnsGroup(creator, group);

        // Создаём User (аккаунт для входа)
        User user = new User();
        PersonNames.apply(user, request.firstName(), request.secondName(), request.thirdName());
        currentSchoolService.setEmail(user, request.email());
        user.setContactEmail(contactEmail);
        credentialMailService.setInitialPassword(user, request.password()); // пусто → временный пароль на почту
        user.setRole(studentRole);

        currentSchoolService.attach(user); // пользователь попадает только в текущую школу

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

        // Учитель редактирует только учеников своего класса и не может перевести их в другой
        User editor = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        checkTeacherOwnsGroup(editor, student.getGroup());
        if (isTeacher(editor) && (student.getGroup() == null || !student.getGroup().getId().equals(request.groupId()))) {
            throw new SecurityException("You can only edit students in your own group.");
        }

        User user = student.getUser();
        PersonNames.apply(user, request.firstName(), request.secondName(), request.thirdName());
        currentSchoolService.setEmail(user, request.email());

        // Меняем пароль только если передан
        if (request.password() != null && !request.password().isBlank()) {
            credentialMailService.setPasswordByOther(user, request.password());
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

    // Новый временный пароль на почту ученика (если письмо не дошло или ученик забыл пароль)
    @Transactional
    public void resetPassword(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        User editor = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        checkTeacherOwnsGroup(editor, student.getGroup());
        credentialMailService.issueTemporaryPassword(student.getUser());
        userRepository.save(student.getUser());
    }

    private boolean isTeacher(User user) {
        return user != null && "TEACHER".equals(user.getRole().getName());
    }

    // Учитель (не админ) может работать только с группой, куратором которой он является
    private void checkTeacherOwnsGroup(User user, GroupEntity group) {
        if (!isTeacher(user)) return;
        Optional<GroupEntity> curatorGroup = groupRepository.findByCuratorId(user.getId());
        if (group == null || curatorGroup.isEmpty() || !curatorGroup.get().getId().equals(group.getId())) {
            throw new SecurityException("You can only manage students in your own group.");
        }
    }

    // Удаляем профиль вместе с аккаунтом — иначе остаётся "пустой" пользователь без профиля
    @Transactional
    public void deleteById(Long id) {
        studentRepository.findById(id).ifPresent(student -> {
            studentRepository.delete(student);
            currentSchoolService.removeUser(student.getUser());
        });
    }
}
