package com.education.school.config;

import com.education.school.entity.User;
import com.education.school.repository.SchoolRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Кастомный провайдер аутентификации для HTML-формы логина
// Расширяет стандартную проверку пароля: добавляет проверку принадлежности к школе (Multi-Tenancy)
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final SchoolRepository schoolRepository;

    public CustomAuthenticationProvider(UserDetailsService userDetailsService,
                                        PasswordEncoder passwordEncoder,
                                        SchoolRepository schoolRepository) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.schoolRepository = schoolRepository;
    }

    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();           // email из формы
        String password = authentication.getCredentials().toString();

        // Читаем schoolName из деталей запроса (заполняется SchoolWebAuthenticationDetailsSource)
        Object details = authentication.getDetails();
        String schoolName = null;
        if (details instanceof SchoolWebAuthenticationDetails) {
            schoolName = ((SchoolWebAuthenticationDetails) details).getSchoolName();
        }

        // Школа обязательна — без неё нельзя проверить принадлежность
        if (schoolName == null || schoolName.isEmpty()) {
            throw new BadCredentialsException("School name is required");
        }

        final String finalSchoolName = schoolName;

        // Загружаем пользователя из БД по email
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Проверяем пароль через BCrypt (сравниваем введённый с хэшем из БД)
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        // Главная проверка Multi-Tenancy: пользователь должен быть зарегистрирован именно в этой школе
        // Одна БД, много школ — каждый пользователь видит только свою школу
        User user = (User) userDetails;
        boolean belongsToSchool = user.getSchools().stream()
                .anyMatch(school -> school.getName().equalsIgnoreCase(finalSchoolName));

        if (!belongsToSchool) {
            throw new BadCredentialsException("User does not belong to school: " + finalSchoolName);
        }

        // Всё проверено — возвращаем объект аутентификации с правами пользователя
        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }

    // Сообщаем Spring Security какой тип Authentication мы умеем обрабатывать
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
