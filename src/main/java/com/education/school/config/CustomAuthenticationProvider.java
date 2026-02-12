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

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final SchoolRepository schoolRepository;

    public CustomAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, SchoolRepository schoolRepository) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.schoolRepository = schoolRepository;
    }

    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        Object details = authentication.getDetails();
        String schoolName = null;
        
        if (details instanceof SchoolWebAuthenticationDetails) {
            schoolName = ((SchoolWebAuthenticationDetails) details).getSchoolName();
        }

        if (schoolName == null || schoolName.isEmpty()) {
            throw new BadCredentialsException("School name is required");
        }

        final String finalSchoolName = schoolName;

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        User user = (User) userDetails;
        boolean belongsToSchool = user.getSchools().stream()
                .anyMatch(school -> school.getName().equalsIgnoreCase(finalSchoolName));

        if (!belongsToSchool) {
            throw new BadCredentialsException("User does not belong to school: " + finalSchoolName);
        }

        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
