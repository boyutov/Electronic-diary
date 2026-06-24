package com.education.school.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

// Фабрика: Spring Security вызывает buildDetails() при каждой попытке аутентификации
// Создаёт SchoolWebAuthenticationDetails, который читает schoolName из параметров запроса
@Component
public class SchoolWebAuthenticationDetailsSource
        implements AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails> {

    @Override
    public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        // Наш объект прочитает schoolName из HTTP-параметра и передаст в провайдер
        return new SchoolWebAuthenticationDetails(context);
    }
}
