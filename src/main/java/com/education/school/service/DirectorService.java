package com.education.school.service;

import com.education.school.dto.DirectorRequest;
import com.education.school.entity.Role;
import com.education.school.entity.User;
import com.education.school.repository.RoleRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DirectorService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User create(DirectorRequest request) {
        Role directorRole = roleRepository.findByName("DIRECTOR")
                .orElseThrow(() -> new IllegalStateException("Role DIRECTOR not found"));

        String creatorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepository.findByEmail(creatorEmail);

        User user = new User();
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(directorRole);
        
        if (creator != null) {
            user.getSchools().addAll(creator.getSchools());
        }

        return userRepository.save(user);
    }
}
