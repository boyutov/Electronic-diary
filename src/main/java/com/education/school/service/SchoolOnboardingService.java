package com.education.school.service;

import com.education.school.dto.PurchaseRequest;
import com.education.school.dto.PurchaseResponse;
import com.education.school.entity.Role;
import com.education.school.entity.School;
import com.education.school.entity.User;
import com.education.school.repository.RoleRepository;
import com.education.school.repository.SchoolRepository;
import com.education.school.repository.UserRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolOnboardingService {

    private static final String DEFAULT_ADMIN_ROLE = "ADMIN";

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;

    public SchoolOnboardingService(
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordGenerator passwordGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordGenerator = passwordGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PurchaseResponse createSchoolAccount(PurchaseRequest request) {
        Role adminRole = roleRepository.findByName(DEFAULT_ADMIN_ROLE)
                .orElseThrow(() -> new IllegalStateException("Role ADMIN not found"));

        String rawPassword = passwordGenerator.generate();

        if (userRepository.findByEmail(request.contactEmail()) != null) {
            throw new IllegalStateException("User with email already exists");
        }

        User adminUser = new User();
        adminUser.setFirstName("School");
        adminUser.setSecondName("Administrator");
        adminUser.setThirdName("");
        adminUser.setLastName(request.schoolName());
        adminUser.setEmail(request.contactEmail());
        adminUser.setPassword(passwordEncoder.encode(rawPassword));
        adminUser.setRole(adminRole);
        adminUser = userRepository.save(adminUser);

        School school = new School();
        school.setName(request.schoolName());
        school.setContactEmail(request.contactEmail());
        school.setContactPhone(request.contactPhone());
        school.setDirectorUser(adminUser);
        schoolRepository.save(school);

        return new PurchaseResponse(school.getId(), adminUser.getId(), rawPassword);
    }
}
