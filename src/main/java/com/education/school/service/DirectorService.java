package com.education.school.service;

import com.education.school.dto.DirectorRequest;
import com.education.school.entity.Role;
import com.education.school.entity.User;
import com.education.school.repository.RoleRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CurrentSchoolService currentSchoolService;
    private final CredentialMailService credentialMailService;

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return currentSchoolService.findUsersByRole("DIRECTOR");
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return currentSchoolService.findUser(id);
    }

    @Transactional
    public User create(DirectorRequest request) {
        Role directorRole = roleRepository.findByName("DIRECTOR")
                .orElseThrow(() -> new IllegalStateException("Role DIRECTOR not found"));

        User user = new User();
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        currentSchoolService.setEmail(user, request.email());
        credentialMailService.setInitialPassword(user, request.password()); // пусто → временный пароль на почту
        user.setRole(directorRole);
        currentSchoolService.attach(user); // без привязки к школе директор не сможет войти

        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, DirectorRequest request) {
        User user = userRepository.findByIdAndSchoolsId(id, currentSchoolService.getId())
                .orElseThrow(() -> new IllegalArgumentException("Director not found"));

        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        currentSchoolService.setEmail(user, request.email());

        if (request.password() != null && !request.password().isBlank()) {
            credentialMailService.setPasswordByOther(user, request.password());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findByIdAndSchoolsId(id, currentSchoolService.getId())
                .orElseThrow(() -> new IllegalArgumentException("Director not found"));
        currentSchoolService.removeUser(user);
    }
}
