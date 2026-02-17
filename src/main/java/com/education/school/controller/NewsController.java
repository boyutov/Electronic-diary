package com.education.school.controller;

import com.education.school.dto.NewsRequest;
import com.education.school.entity.News;
import com.education.school.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{id}")
    @Operation(summary = "Получить новость по ID")
    public ResponseEntity<News> findById(@PathVariable String schoolName, @PathVariable Integer id) {
        News news = service.findById(id);
        return news != null ? ResponseEntity.ok(news) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Опубликовать новость")
    public News create(@PathVariable String schoolName, @Valid @RequestBody NewsRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить новость")
    public News update(@PathVariable String schoolName, @PathVariable Integer id, @Valid @RequestBody NewsRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить новость")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
