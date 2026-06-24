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

// Обработчик успешного входа через HTML-форму
// Определяет роль пользователя и делает редирект на нужную панель управления
@Component
public class RoleBasedSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // Получаем schoolName из деталей запроса
        String schoolName = null;
        Object details = authentication.getDetails();
        if (details instanceof SchoolWebAuthenticationDetails) {
            schoolName = ((SchoolWebAuthenticationDetails) details).getSchoolName();
        }

        // Сохраняем школу в сессию — используется в шаблонах Thymeleaf
        if (schoolName != null) {
            request.getSession().setAttribute("currentSchool", schoolName);
        }

        // Префикс URL: "/SchoolA" или пустая строка
        String prefix = (schoolName != null && !schoolName.isEmpty())
                ? "/" + schoolName.replaceAll("/", "")
                : "";

        // Собираем все роли пользователя в набор
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Редирект по роли: каждая роль → своя панель управления
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

        // Роль неизвестна — на главную страницу
        response.sendRedirect("/");
    }
}
