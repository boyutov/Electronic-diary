package com.education.school.service;

import com.education.school.dto.PurchaseRequest;
import com.education.school.dto.PurchaseResponse;
import com.education.school.dto.SchoolRegistrationRequest;
import com.education.school.entity.Role;
import com.education.school.entity.School;
import com.education.school.entity.User;
import com.education.school.repository.RoleRepository;
import com.education.school.repository.SchoolRepository;
import com.education.school.repository.UserRepository;
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
        String rawPassword = passwordGenerator.generate();

        School school = new School();
        school.setName(request.schoolName());
        school.setContactEmail(request.contactEmail());
        school.setContactPhone(request.contactPhone());
        school.setAccessPasswordHash(passwordEncoder.encode(rawPassword));
        schoolRepository.save(school);

        return new PurchaseResponse(school.getId(), null, rawPassword);
    }

    @Transactional
    public PurchaseResponse completeSchoolRegistration(SchoolRegistrationRequest request) {
        Role adminRole = roleRepository.findByName(DEFAULT_ADMIN_ROLE)
                .orElseThrow(() -> new IllegalStateException("Role ADMIN not found"));

        School school = schoolRepository.findAll()
                .stream()
                .filter(entry -> entry.getAccessPasswordHash() != null)
                .filter(entry -> passwordEncoder.matches(request.schoolPassword(), entry.getAccessPasswordHash()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Invalid school password"));

        if (school.getDirectorUser() != null) {
            throw new IllegalStateException("School already activated");
        }

        if (userRepository.findByEmail(request.email()) != null) {
            throw new IllegalStateException("User with email already exists");
        }

        User adminUser = new User();
        adminUser.setFirstName(request.firstName());
        adminUser.setSecondName(request.secondName());
        adminUser.setThirdName(request.thirdName());
        adminUser.setEmail(request.email());
        adminUser.setPassword(passwordEncoder.encode(request.schoolPassword()));
        adminUser.setRole(adminRole);
        adminUser = userRepository.save(adminUser);

        school.setDirectorUser(adminUser);
        schoolRepository.save(school);

        return new PurchaseResponse(school.getId(), adminUser.getId(), null);
    }
}
