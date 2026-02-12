package com.education.school.service;

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

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    @Transactional
    public Student create(StudentRequest request) {
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("Role STUDENT not found"));

        String creatorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepository.findByEmail(creatorEmail);

        GroupEntity group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Security check
        if (creator.getRole().getName().equals("TEACHER")) {
            Optional<GroupEntity> curatorGroup = groupRepository.findByCuratorId(creator.getId());
            if (curatorGroup.isEmpty() || !curatorGroup.get().getId().equals(group.getId())) {
                throw new SecurityException("You can only create students in your own group.");
            }
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(studentRole);
        
        if (creator != null) {
            user.getSchools().addAll(creator.getSchools());
        }
        
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setAge(request.age());
        student.setGroup(group);
        student.setCurator(group.getCurator()); // Устанавливаем куратора из группы
        student.setEmail(request.email());

        return studentRepository.save(student);
    }

    public Student findById(Long id) {
        return studentRepository.findById(id).orElse(null);
    }

    public void deleteById(Long id) {
        studentRepository.deleteById(id);
    }
}
