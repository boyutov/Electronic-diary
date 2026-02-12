package com.education.school.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        
        String schoolName = null;
        Object details = authentication.getDetails();
        if (details instanceof SchoolWebAuthenticationDetails) {
            schoolName = ((SchoolWebAuthenticationDetails) details).getSchoolName();
        }
        
        if (schoolName != null) {
            request.getSession().setAttribute("currentSchool", schoolName);
        }

        String prefix = (schoolName != null && !schoolName.isEmpty()) ? "/" + schoolName.replaceAll("/", "") : "";

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (roles.contains("ADMIN")) {
            response.sendRedirect(prefix + "/admin");
            return;
        }
        if (roles.contains("DIRECTOR")) {
            response.sendRedirect(prefix + "/director");
            return;
        }
        if (roles.contains("TEACHER")) {
            response.sendRedirect(prefix + "/teacher");
            return;
        }
        if (roles.contains("PARENT")) {
            response.sendRedirect(prefix + "/parent");
            return;
        }
        if (roles.contains("STUDENT")) {
            response.sendRedirect(prefix + "/student");
            return;
        }
        response.sendRedirect("/");
    }
}
