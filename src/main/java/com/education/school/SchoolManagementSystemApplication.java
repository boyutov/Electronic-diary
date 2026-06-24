package com.education.school;

import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Подключаем Swagger — в браузере отображается как заголовок документации API
@OpenAPIDefinition(info = @Info(title = "School API"))

// Главная аннотация Spring Boot: включает автоконфигурацию, сканирование компонентов и запуск сервера
@SpringBootApplication
public class SchoolManagementSystemApplication {

    // Точка входа — JVM запускает именно этот метод
    public static void main(String[] args) {
        // Поднимаем Spring-контекст: стартует сервер, подключается БД, регистрируются все бины
        SpringApplication.run(SchoolManagementSystemApplication.class, args);
    }
}
