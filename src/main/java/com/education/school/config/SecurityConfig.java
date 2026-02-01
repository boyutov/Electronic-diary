package com.education.school.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final RoleBasedSuccessHandler roleBasedSuccessHandler;

    public SecurityConfig(RoleBasedSuccessHandler roleBasedSuccessHandler) {
        this.roleBasedSuccessHandler = roleBasedSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(roleBasedSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout.logoutUrl("/logout"))
                .authorizeHttpRequests(auth -> auth
                        // разрешаем главную страницу и статику (если будет)
                        .requestMatchers(
                                "/", "/index", "/about", "/pricing", "/login", "/activate", "/error",
                                "/css/**", "/js/**", "/images/**", "/webjars/**"
                        ).permitAll()

                        // разрешаем swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // пример: разрешаем конкретный POST (если нужно)
                        .requestMatchers(HttpMethod.POST, "/api/students", "/api/purchase", "/api/purchase/register")
                        .permitAll()

                        .requestMatchers("/admin", "/admin/**", "/api/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/director", "/director/**").hasAnyAuthority("ADMIN", "DIRECTOR")
                        .requestMatchers("/teacher", "/teacher/**").hasAnyAuthority("ADMIN", "TEACHER")
                        .requestMatchers("/student", "/student/**").hasAnyAuthority("ADMIN", "STUDENT")
                        .requestMatchers("/parent", "/parent/**").hasAnyAuthority("ADMIN", "PARENT")
                        .requestMatchers("/api/director/**").hasAnyAuthority("ADMIN", "DIRECTOR")
                        .requestMatchers("/api/teacher/**").hasAnyAuthority("ADMIN", "TEACHER")
                        .requestMatchers("/api/parent/**").hasAnyAuthority("ADMIN", "PARENT")
                        .requestMatchers("/api/student/**").hasAnyAuthority("ADMIN", "STUDENT")
                        .requestMatchers("/api/**").authenticated()

                        // всё остальное — под авторизацией
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
