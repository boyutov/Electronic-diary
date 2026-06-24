package com.education.school.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Главный класс конфигурации безопасности — все правила доступа к URL
@Configuration
@EnableWebSecurity  // активируем Spring Security
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    // Настраиваем цепочку фильтров безопасности
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF отключаем — при JWT не нужен (CSRF атаки возможны только при сессиях + cookies)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)

                // STATELESS: сервер не хранит сессии — каждый запрос аутентифицируется по токену
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Подключаем наш провайдер (проверяет пароль + принадлежность к школе)
                .authenticationProvider(authenticationProvider())

                // JWT-фильтр должен отработать ДО стандартного фильтра логина
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // Отключаем стандартную Spring-форму логина — у нас своя HTML-страница
                .formLogin(AbstractHttpConfigurer::disable)

                // При logout перенаправляем на страницу логина
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login"))

                .authorizeHttpRequests(auth -> auth
                        // Публичные страницы — открыты всем без токена
                        .requestMatchers(
                                "/", "/index", "/about", "/pricing", "/login", "/activate", "/error",
                                "/css/**", "/js/**", "/images/**", "/webjars/**"
                        ).permitAll()

                        // Swagger — открыт для тестирования API
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Публичные API: вход и регистрация школы не требуют токена
                        .requestMatchers("/api/auth/**", "/api/purchase/**").permitAll()

                        // HTML-страницы — permitAll, реальная защита идёт через JWT при API-запросах
                        .requestMatchers("/admin", "/admin/**").permitAll()
                        .requestMatchers("/*/admin", "/*/admin/**").permitAll()
                        .requestMatchers("/*/director", "/*/director/**").permitAll()
                        .requestMatchers("/*/teacher", "/*/teacher/**").permitAll()
                        .requestMatchers("/*/student", "/*/student/**").permitAll()
                        .requestMatchers("/*/parent", "/*/parent/**").permitAll()
                        .requestMatchers("/*/profile").permitAll()
                        .requestMatchers("/*/schedule").permitAll()
                        .requestMatchers("/*/polls").permitAll()
                        .requestMatchers("/*/complaints").permitAll()
                        .requestMatchers("/*/canteen").permitAll()
                        .requestMatchers("/*/news").permitAll()
                        .requestMatchers("/*/attendance").permitAll()
                        .requestMatchers("/*/exams").permitAll()
                        .requestMatchers("/*/courses").permitAll()
                        .requestMatchers("/*/notifications").permitAll()
                        .requestMatchers("/*/admin/grading").permitAll()
                        .requestMatchers("/*/grades").permitAll()
                        .requestMatchers("/*/teacher/marks").permitAll()
                        .requestMatchers("/*/curator/**").permitAll()

                        // API управления администраторами — только роль ADMIN
                        .requestMatchers("/api/*/admins/**").hasAuthority("ADMIN")
                        // API директора — ADMIN или DIRECTOR
                        .requestMatchers("/api/*/directors/**").hasAnyAuthority("ADMIN", "DIRECTOR")
                        // Все остальные API — любой авторизованный пользователь
                        .requestMatchers("/api/**").authenticated()

                        // Все прочие пути (HTML-страницы) — открыты
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    // Провайдер аутентификации: связываем UserDetailsService (загрузка из БД) с BCrypt (проверка пароля)
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(passwordEncoder());
        authProvider.setUserDetailsService(userDetailsService);
        return authProvider;
    }

    // AuthenticationManager нужен в AuthController для ручной аутентификации через REST API
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // BCrypt — стандарт хэширования паролей с солью. Нельзя расшифровать, можно только сравнить
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
