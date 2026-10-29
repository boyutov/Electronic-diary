package com.education.school.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// Класс конфигурации — здесь объявляем Spring-бины (объекты управляемые контейнером)
@Configuration
public class AIConfig {

    // Старые сервисы используют этот клиент; внешние запросы запрещены.
    // @Bean: Spring создаёт один экземпляр и передаёт его везде, где он нужен
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate client = new RestTemplate();
        client.setInterceptors(java.util.List.of((request, body, execution) -> {
            throw new java.io.IOException("External AI requests are disabled");
        }));
        return client;
    }
}
