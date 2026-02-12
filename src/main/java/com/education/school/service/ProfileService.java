package com.education.school.service;

import com.education.school.dto.CuratorProfileDto;
import com.education.school.dto.ProfileUpdateRequest;
import com.education.school.entity.GroupEntity;
import com.education.school.entity.User;
import com.education.school.repository.GroupRepository;
import com.education.school.repository.UserRepository;
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
    private final PasswordEncoder passwordEncoder;

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
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User updateProfile(ProfileUpdateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        
        user.setFirstName(request.firstName());
        user.setSecondName(request.secondName());
        user.setThirdName(request.thirdName());
        user.setEmail(request.email());
        
        if (request.newPassword() != null && !request.newPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }
        
        return userRepository.save(user);
    }
}
