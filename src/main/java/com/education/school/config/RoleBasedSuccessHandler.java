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
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (roles.contains("ADMIN")) {
            response.sendRedirect("/admin");
            return;
        }
        if (roles.contains("DIRECTOR")) {
            response.sendRedirect("/director");
            return;
        }
        if (roles.contains("TEACHER")) {
            response.sendRedirect("/teacher");
            return;
        }
        if (roles.contains("PARENT")) {
            response.sendRedirect("/parent");
            return;
        }
        if (roles.contains("STUDENT")) {
            response.sendRedirect("/student");
            return;
        }
        response.sendRedirect("/");
    }
}
