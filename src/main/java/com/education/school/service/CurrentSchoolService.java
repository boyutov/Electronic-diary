package com.education.school.service;

import com.education.school.config.TenantContext;
import com.education.school.entity.School;
import com.education.school.entity.User;
import com.education.school.repository.SchoolRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// Доступ к школе текущего запроса (Multi-Tenancy)
// Школьные сущности фильтруются Hibernate автоматически (@TenantId), а пользователи общие —
// поэтому всё, что касается users, проходит через этот сервис
@Service
@RequiredArgsConstructor
public class CurrentSchoolService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    public Long getId() {
        return TenantContext.getRequiredSchoolId();
    }

    public School get() {
        return schoolRepository.getReferenceById(getId());
    }

    // Новый пользователь должен состоять в текущей школе — иначе он не сможет войти
    public void attach(User user) {
        user.getSchools().add(get());
    }

    public List<User> findUsers() {
        return userRepository.findAllBySchoolsId(getId());
    }

    public List<User> findUsersByRole(String roleName) {
        return userRepository.findAllBySchoolsIdAndRoleName(getId(), roleName);
    }

    // Пользователь по ID, но только из текущей школы — чужого не найдём
    public User findUser(Long id) {
        return userRepository.findByIdAndSchoolsId(id, getId()).orElse(null);
    }

    // Логин (email) уникален во всей системе: дубликат сломал бы вход обоим пользователям
    public void setEmail(User user, String email) {
        String normalized = email == null ? null : email.trim();
        boolean changed = user.getEmail() == null || !user.getEmail().equalsIgnoreCase(normalized);
        if (changed && normalized != null && userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new IllegalArgumentException("Пользователь с логином " + normalized + " уже существует");
        }
        user.setEmail(normalized);
    }

    // Удаление из текущей школы: если человек состоит и в других школах — только отвязываем
    public void removeUser(User user) {
        boolean inOtherSchools = user.getSchools().stream().anyMatch(s -> !s.getId().equals(getId()));
        if (inOtherSchools) {
            user.getSchools().removeIf(s -> s.getId().equals(getId()));
            userRepository.save(user);
        } else {
            userRepository.delete(user);
        }
    }
}
