package com.education.school.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// Класс конфигурации — здесь объявляем Spring-бины (объекты управляемые контейнером)
@Configuration
public class AIConfig {

    // RestTemplate — HTTP-клиент для отправки запросов к OpenAI API
    // @Bean: Spring создаёт один экземпляр и передаёт его везде, где он нужен
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
