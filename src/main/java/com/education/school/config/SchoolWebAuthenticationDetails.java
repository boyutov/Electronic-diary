package com.education.school.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

// Расширяем стандартные детали аутентификации — добавляем поле schoolName
// Объект передаётся в CustomAuthenticationProvider через authentication.getDetails()
public class SchoolWebAuthenticationDetails extends WebAuthenticationDetails {

    private final String schoolName;

    public SchoolWebAuthenticationDetails(HttpServletRequest request) {
        super(request); // сохраняем стандартные данные: IP-адрес, sessionId
        // Читаем schoolName из параметра формы логина (hidden input или обычный input)
        this.schoolName = request.getParameter("schoolName");
    }

    public String getSchoolName() {
        return schoolName;
    }
}
