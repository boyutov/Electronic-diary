package com.education.school.service;

import com.education.school.dto.AdminRequest;
import com.education.school.entity.Role;
import com.education.school.entity.User;
import com.education.school.repository.RoleRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Сервис управления администраторами школы
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CurrentSchoolService currentSchoolService;
    private final CredentialMailService credentialMailService;

    // Получить всех администраторов — readOnly транзакция быстрее (не блокирует строки)
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return currentSchoolService.findUsersByRole("ADMIN");
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return currentSchoolService.findUser(id);
    }

    // Создать нового администратора
    @Transactional
    public User create(AdminRequest request) {
        // Находим роль ADMIN в БД (роли создаются через Flyway-миграции при старте)
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Role ADMIN not found"));

        User user = new User();
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        currentSchoolService.setEmail(user, request.email());
        credentialMailService.setInitialPassword(user, request.password()); // пусто → временный пароль на почту
        user.setRole(adminRole);

        // Новый администратор попадает в текущую школу — Multi-Tenancy
        currentSchoolService.attach(user);

        return userRepository.save(user);
    }

    // Обновить данные администратора
    @Transactional
    public User update(Long id, AdminRequest request) {
        User user = userRepository.findByIdAndSchoolsId(id, currentSchoolService.getId())
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        currentSchoolService.setEmail(user, request.email());

        // Обновляем пароль только если он передан — иначе оставляем старый
        if (request.password() != null && !request.password().isBlank()) {
            credentialMailService.setPasswordByOther(user, request.password());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findByIdAndSchoolsId(id, currentSchoolService.getId())
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        currentSchoolService.removeUser(user);
    }
}
