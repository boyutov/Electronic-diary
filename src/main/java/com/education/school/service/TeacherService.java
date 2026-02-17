package com.education.school.service;

import com.education.school.dto.TeacherRequest;
import com.education.school.dto.TeacherResponse;
import com.education.school.entity.*;
import com.education.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final DisciplineRepository disciplineRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll() {
        return teacherRepository.findAll().stream()
                .map(TeacherResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAllWithGroup() {
        return teacherRepository.findAll().stream()
                .filter(teacher -> Boolean.TRUE.equals(teacher.getHasGroup()))
                .map(TeacherResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Teacher findById(Integer id) {
        return teacherRepository.findById(id).orElse(null);
    }

    @Transactional
    public Teacher create(TeacherRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required for new teacher");
        }

        Role teacherRole = roleRepository.findByName("TEACHER")
                .orElseThrow(() -> new IllegalStateException("Role TEACHER not found"));

        String creatorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepository.findByEmail(creatorEmail);

        User user = new User();
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(teacherRole);
        
        if (creator != null) {
            user.getSchools().addAll(creator.getSchools());
        }

        user = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setPhone(request.phone());
        teacher.setBio(request.bio());
        teacher.setHasOffice(request.hasOffice());
        teacher.setOffice(request.office());

        if (Boolean.TRUE.equals(request.hasGroup()) && request.groupId() != null) {
            GroupEntity group = groupRepository.findById(request.groupId())
                    .orElseThrow(() -> new IllegalArgumentException("Group not found"));
            
            teacher.setHasGroup(true);
            teacher.setGroup(group);
            
            group.setCurator(user);
            groupRepository.save(group);
        } else {
            teacher.setHasGroup(false);
        }

        if (request.disciplineIds() != null && !request.disciplineIds().isEmpty()) {
            List<Discipline> disciplines = disciplineRepository.findAllById(request.disciplineIds());
            teacher.setDisciplines(new HashSet<>(disciplines));
        }

        return teacherRepository.save(teacher);
    }

    @Transactional
    public Teacher update(Integer id, TeacherRequest request) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        
        User user = teacher.getUser();
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        user.setEmail(request.email());
        
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        userRepository.save(user);

        teacher.setPhone(request.phone());
        teacher.setBio(request.bio());
        teacher.setHasOffice(request.hasOffice());
        teacher.setOffice(request.office());

        if (Boolean.TRUE.equals(request.hasGroup()) && request.groupId() != null) {
            GroupEntity group = groupRepository.findById(request.groupId())
                    .orElseThrow(() -> new IllegalArgumentException("Group not found"));
            
            teacher.setHasGroup(true);
            teacher.setGroup(group);
            group.setCurator(user);
            groupRepository.save(group);
        } else {
            teacher.setHasGroup(false);
            teacher.setGroup(null);
        }

        if (request.disciplineIds() != null) {
            List<Discipline> disciplines = disciplineRepository.findAllById(request.disciplineIds());
            teacher.setDisciplines(new HashSet<>(disciplines));
        }

        return teacherRepository.save(teacher);
    }

    @Transactional
    public void delete(Integer id) {
        teacherRepository.deleteById(id);
    }
}
