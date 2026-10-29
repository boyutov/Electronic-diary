package com.education.school.service;

import com.education.school.dto.CuratorProfileDto;
import com.education.school.dto.PasswordChangeRequest;
import com.education.school.dto.ProfileUpdateRequest;
import com.education.school.dto.UserProfileDto;
import com.education.school.entity.GroupEntity;
import com.education.school.entity.User;
import com.education.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentSchoolService currentSchoolService;

    @Transactional(readOnly = true)
    public Optional<CuratorProfileDto> getCuratorProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email);

        if (currentUser == null || !currentUser.getRole().getName().equals("TEACHER")) {
            return Optional.empty();
        }

        Optional<GroupEntity> group = groupRepository.findByCuratorId(currentUser.getId());

        return group.map(groupEntity -> CuratorProfileDto.from(currentUser, groupEntity));
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        UserProfileDto dto = UserProfileDto.from(user);
        
        String role = user.getRole().getName();
        if ("TEACHER".equals(role)) {
            teacherRepository.findByUserId(user.getId()).ifPresent(t -> {
                dto.setPhone(t.getPhone());
                dto.setBio(t.getBio());
                dto.setOffice(t.getOffice());
            });
        } else if ("STUDENT".equals(role)) {
            studentRepository.findByUserId(user.getId()).ifPresent(s -> dto.setPhone(s.getPhone()));
        } else if ("PARENT".equals(role)) {
            parentRepository.findByUserEmail(user.getEmail()).ifPresent(p -> dto.setPhone(p.getPhone()));
        }
        
        return dto;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    // Смена своего пароля: проверяем текущий, новый должен отличаться от него и от логина
    @Transactional
    public User changePassword(PasswordChangeRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Текущий пароль указан неверно");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new IllegalArgumentException("Новый пароль должен отличаться от текущего");
        }
        if (request.newPassword().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Пароль не должен совпадать с логином");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        return userRepository.save(user);
    }

    @Transactional
    public UserProfileDto updateProfile(ProfileUpdateRequest request) {
        if (request.newPassword() != null && !request.newPassword().isEmpty()) {
            throw new IllegalArgumentException("Для смены пароля используйте форму смены пароля с проверкой текущего пароля");
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        
        // ФИО ученика ведёт школа (оно в журнале и документах) — сам ученик его не меняет
        if ("STUDENT".equals(user.getRole().getName())) {
            boolean namesChanged = !java.util.Objects.equals(user.getFirstName(), request.firstName())
                    || !java.util.Objects.equals(user.getSecondName(), request.secondName())
                    || !java.util.Objects.equals(blankToNull(user.getThirdName()), blankToNull(request.thirdName()));
            if (namesChanged) {
                throw new SecurityException("Ученик не может менять ФИО — обратитесь к классному руководителю");
            }
        } else {
            PersonNames.apply(user, request.firstName(), request.secondName(), request.thirdName());
        }
        currentSchoolService.setEmail(user, request.email()); // занятый логин → понятная ошибка, а не 500

        
        User savedUser = userRepository.save(user);
        UserProfileDto dto = UserProfileDto.from(savedUser);
        
        String role = savedUser.getRole().getName();
        if ("TEACHER".equals(role)) {
            teacherRepository.findByUserId(savedUser.getId()).ifPresent(teacher -> {
                teacher.setPhone(request.phone());
                teacher.setBio(request.bio());
                teacher.setOffice(request.office());
                teacherRepository.save(teacher);
                dto.setPhone(teacher.getPhone());
                dto.setBio(teacher.getBio());
                dto.setOffice(teacher.getOffice());
            });
        } else if ("STUDENT".equals(role)) {
            studentRepository.findByUserId(savedUser.getId()).ifPresent(student -> {
                student.setPhone(request.phone());
                studentRepository.save(student);
                dto.setPhone(student.getPhone());
            });
        } else if ("PARENT".equals(role)) {
            parentRepository.findByUserEmail(savedUser.getEmail()).ifPresent(parent -> {
                parent.setPhone(request.phone());
                parentRepository.save(parent);
                dto.setPhone(parent.getPhone());
            });
        }
        
        return dto;
    }
}
