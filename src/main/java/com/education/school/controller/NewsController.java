package com.education.school.controller;

import com.education.school.dto.NewsRequest;
import com.education.school.dto.NewsResponse;
import com.education.school.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{schoolName}/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "Управление новостями")
public class NewsController {

    private final NewsService service;

    @GetMapping
    @Operation(summary = "Получить все новости")
    public List<NewsResponse> findAll(@PathVariable String schoolName) {
        return service.findAll().stream().map(NewsResponse::from).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить новость по ID")
    public ResponseEntity<NewsResponse> findById(@PathVariable String schoolName, @PathVariable Integer id) {
        return service.findById(id) != null
                ? ResponseEntity.ok(NewsResponse.from(service.findById(id)))
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Опубликовать новость")
    public NewsResponse create(@PathVariable String schoolName, @Valid @RequestBody NewsRequest request) {
        return NewsResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить новость")
    public NewsResponse update(@PathVariable String schoolName, @PathVariable Integer id, @Valid @RequestBody NewsRequest request) {
        return NewsResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить новость")
    public ResponseEntity<Void> delete(@PathVariable String schoolName, @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
