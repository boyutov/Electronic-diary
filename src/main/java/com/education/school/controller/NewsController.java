package com.education.school.controller;

import com.education.school.dto.NewsRequest;
import com.education.school.entity.News;
import com.education.school.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{schoolName}/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "Управление новостями")
public class NewsController {

    private final NewsService service;

    @GetMapping
    @Operation(summary = "Получить все новости")
    public List<News> findAll(@PathVariable String schoolName) {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Опубликовать новость")
    public News create(@PathVariable String schoolName, @Valid @RequestBody NewsRequest request) {
        return service.create(request);
    }
}
