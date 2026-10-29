package com.education.school.config;

import com.education.school.entity.School;
import com.education.school.entity.User;
import com.education.school.repository.SchoolRepository;
import com.education.school.repository.UserRepository;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

// JWT-фильтр — перехватывает каждый HTTP-запрос ровно один раз
// Проверяет наличие Bearer-токена и аутентифицирует пользователя
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

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

                // Multi-Tenancy: токен школы A нельзя использовать для запросов /api/{школа B}/...
                String tokenSchool = jwtService.extractSchoolName(jwt);
                if (!schoolMatchesPath(request, tokenSchool)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token does not belong to this school");
                    return;
                }

                // Школа из токена должна существовать, а пользователь — по-прежнему в ней состоять
                Long schoolId = schoolRepository.findByNameIgnoreCase(tokenSchool).map(School::getId).orElse(null);
                if (schoolId == null || !userRepository.existsByIdAndSchoolsId(((User) userDetails).getId(), schoolId)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "User does not belong to this school");
                    return;
                }

                // Проверяем токен: совпадает email + не истёк срок
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // С этого момента Hibernate видит только данные этой школы
                    TenantContext.setSchoolId(schoolId);

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
            TenantContext.clear();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // Передаём запрос дальше по цепочке фильтров; школу сбрасываем, т.к. поток вернётся в пул
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    // Для путей вида /api/{schoolName}/... школа в URL должна совпадать со школой из токена
    private boolean schoolMatchesPath(HttpServletRequest request, String tokenSchool) {
        String[] parts = request.getRequestURI().substring(request.getContextPath().length()).split("/");
        // parts: ["", "api", "{schoolName}", ...]
        if (parts.length < 3 || !"api".equals(parts[1])) {
            return true;
        }
        String pathSchool = URLDecoder.decode(parts[2], StandardCharsets.UTF_8); // slug может быть кириллицей
        if ("auth".equals(pathSchool) || "purchase".equals(pathSchool)) {
            return true;
        }
        return tokenSchool != null && tokenSchool.equalsIgnoreCase(pathSchool);
    }
}
