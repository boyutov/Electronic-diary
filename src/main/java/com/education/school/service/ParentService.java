package com.education.school.service;

import com.education.school.dto.ParentRequest;
import com.education.school.entity.Parent;
import com.education.school.entity.Role;
import com.education.school.entity.User;
import com.education.school.repository.ParentRepository;
import com.education.school.repository.RoleRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Parent> findAll() {
        return parentRepository.findAll();
    }

    @Transactional
    public Parent create(ParentRequest request) {
        Role parentRole = roleRepository.findByName("PARENT")
                .orElseThrow(() -> new IllegalStateException("Role PARENT not found"));

        String creatorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepository.findByEmail(creatorEmail);

        User user = new User();
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(parentRole);
        
        if (creator != null) {
            user.getSchools().addAll(creator.getSchools());
        }

        user = userRepository.save(user);

        Parent parent = new Parent();
        parent.setUser(user);
        parent.setPhone(request.phone());

        return parentRepository.save(parent);
    }
}
