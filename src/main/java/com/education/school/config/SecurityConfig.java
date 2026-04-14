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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable) // Отключаем стандартную форму логина
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login"))
                .authorizeHttpRequests(auth -> auth
                        // разрешаем главную страницу и статику
                        .requestMatchers(
                                "/", "/index", "/about", "/pricing", "/login", "/activate", "/error",
                                "/css/**", "/js/**", "/images/**", "/webjars/**"
                        ).permitAll()

                        // разрешаем swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Эндпоинты авторизации и публичные API
                        .requestMatchers("/api/auth/**", "/api/purchase/**").permitAll()

                        // Страницы приложения — разрешаем все не-API пути, JWT проверяется на уровне API
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
                        .requestMatchers("/*/attendance").permitAll()
                        .requestMatchers("/*/exams").permitAll()
                        .requestMatchers("/*/curator/**").permitAll()

                        // Защита API с проверкой ролей
                        .requestMatchers("/api/*/admins/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/*/directors/**").hasAnyAuthority("ADMIN", "DIRECTOR")
                        .requestMatchers("/api/**").authenticated()

                        // все остальные пути (HTML-страницы) — разрешаем
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
