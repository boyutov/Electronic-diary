package com.education.school.service;

import com.education.school.dto.NewsRequest;
import com.education.school.entity.News;
import com.education.school.entity.Teacher;
import com.education.school.entity.User;
import com.education.school.repository.NewsRepository;
import com.education.school.repository.TeacherRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public List<News> findAll() {
        return newsRepository.findAll();
    }

    public News findById(Integer id) {
        return newsRepository.findById(id).orElse(null);
    }

    @Transactional
    public News create(NewsRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User createdByUser = userRepository.findByEmail(email);

        News news = new News();
        news.setTitle(request.title());
        news.setText(request.text());
        news.setCreatedAt(LocalDateTime.now());
        news.setCreatedByUser(createdByUser);

        if (request.teacherId() != null) {
            Teacher teacher = teacherRepository.findById(request.teacherId())
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
            news.setTeacher(teacher);
        }

        return newsRepository.save(news);
    }

    @Transactional
    public News update(Integer id, NewsRequest request) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("News not found"));

        news.setTitle(request.title());
        news.setText(request.text());

        if (request.teacherId() != null) {
            Teacher teacher = teacherRepository.findById(request.teacherId())
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
            news.setTeacher(teacher);
        } else {
            news.setTeacher(null);
        }

        return newsRepository.save(news);
    }

    @Transactional
    public void delete(Integer id) {
        newsRepository.deleteById(id);
    }
}
