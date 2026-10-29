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

    // Регистрация школы бесплатная — цена всегда 0
    public double calculatePrice(int students, int months, String promoCode) {
        return 0.0;
    }

    @Transactional
    public PurchaseResponse createSchoolAccount(PurchaseRequest request) {
        // slug — это идентификатор школы в URL и токене, поэтому он должен быть уникальным
        String slug = toSlug(request.schoolName());
        if (slug.isEmpty() || schoolRepository.findByNameIgnoreCase(slug).isPresent()) {
            throw new IllegalArgumentException(slug.isEmpty()
                    ? "Укажите название школы латиницей"
                    : "Школа с таким названием уже зарегистрирована");
        }

        String rawPassword = passwordGenerator.generate();

        School school = new School();
        school.setName(slug);
        school.setContactEmail(request.contactEmail());
        school.setContactPhone(request.contactPhone());
        school.setAccessPasswordHash(passwordEncoder.encode(rawPassword));
        schoolRepository.save(school);

        return new PurchaseResponse(school.getId(), null, rawPassword);
    }

    private String toSlug(String name) {
        if (name == null) return "";
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9а-яё]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
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

        if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new IllegalStateException("User with email already exists");
        }

        User adminUser = new User();
        PersonNames.apply(adminUser, request.firstName(), request.secondName(), request.thirdName());
        adminUser.setEmail(request.email());
        adminUser.setPassword(passwordEncoder.encode(request.schoolPassword()));
        adminUser.setMustChangePassword(true); // код активации выдан системой — при первом входе меняется
        adminUser.setRole(adminRole);
        
        adminUser.getSchools().add(school);
        
        adminUser = userRepository.save(adminUser);

        school.setDirectorUser(adminUser);
        school.getUsers().add(adminUser);

        schoolRepository.save(school);

        return new PurchaseResponse(school.getId(), adminUser.getId(), null);
    }
}
