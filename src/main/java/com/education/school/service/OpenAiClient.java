package com.education.school.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Единственная точка выхода во внешний ИИ для анализа и генератора.
 * Вызывающий код обязан замаскировать персональные данные (AiPrivacy) до вызова:
 * сюда должны попадать только обезличенный текст запроса и агрегаты.
 */
@Component
public class OpenAiClient {

    private final ObjectMapper objectMapper;
    private final RestTemplate http;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    public OpenAiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);
        this.http = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String complete(String systemPrompt, String userPrompt, int maxTokens) {
        if (!isConfigured()) throw new IllegalStateException("OPENAI_API_KEY не настроен");

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.1);
        body.put("max_tokens", maxTokens);
        body.put("store", false); // не хранить запросы на стороне OpenAI
        var messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        try {
            String response = http.exchange("https://api.openai.com/v1/chat/completions", HttpMethod.POST,
                    new HttpEntity<>(body.toString(), headers), String.class).getBody();
            JsonNode content = objectMapper.readTree(response).path("choices").path(0).path("message").path("content");
            return content.asText();
        } catch (Exception e) {
            throw new IllegalStateException("ИИ временно недоступен");
        }
    }
}
