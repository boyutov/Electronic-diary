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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public List<News> findAll() {
        return newsRepository.findAll();
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
}
