package com.education.school.service;

import com.education.school.dto.AdminRequest;
import com.education.school.entity.Role;
import com.education.school.entity.User;
import com.education.school.repository.RoleRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Сервис управления администраторами школы
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Получить всех администраторов — readOnly транзакция быстрее (не блокирует строки)
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAllByRoleName("ADMIN");
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // Создать нового администратора
    @Transactional
    public User create(AdminRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required for new admin");
        }

        // Находим роль ADMIN в БД (роли создаются через Flyway-миграции при старте)
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Role ADMIN not found"));

        // Определяем кто создаёт — берём из текущего контекста безопасности
        String creatorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepository.findByEmail(creatorEmail);

        User user = new User();
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password())); // хэшируем пароль перед сохранением
        user.setRole(adminRole);

        // Новый администратор наследует школы создателя — Multi-Tenancy
        if (creator != null) {
            user.getSchools().addAll(creator.getSchools());
        }

        return userRepository.save(user);
    }

    // Обновить данные администратора
    @Transactional
    public User update(Long id, AdminRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        user.setEmail(request.email());

        // Обновляем пароль только если он передан — иначе оставляем старый
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
