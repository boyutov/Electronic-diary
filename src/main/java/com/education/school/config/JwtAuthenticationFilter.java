package com.education.school.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// JWT-фильтр — перехватывает каждый HTTP-запрос ровно один раз
// Проверяет наличие Bearer-токена и аутентифицирует пользователя
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Читаем заголовок Authorization из HTTP-запроса
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Если заголовка нет или не начинается с "Bearer " — пропускаем запрос без аутентификации
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Обрезаем "Bearer " (7 символов) — остаётся чистый токен
        jwt = authHeader.substring(7);
        try {
            // Извлекаем email из токена
            userEmail = jwtService.extractUsername(jwt);

            // Если email есть И пользователь ещё не аутентифицирован в этом запросе — проверяем токен
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Загружаем пользователя из БД по email
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Проверяем токен: совпадает email + не истёк срок
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Создаём объект аутентификации с ролями пользователя
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,                          // credentials не нужны — токен уже проверен
                            userDetails.getAuthorities()   // роли: ADMIN, TEACHER и т.д.
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Помещаем аутентификацию в контекст — теперь Spring знает кто делает запрос
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Токен повреждён или подпись не совпадает — возвращаем 401 Unauthorized
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // Передаём запрос дальше по цепочке фильтров
        filterChain.doFilter(request, response);
    }
}
