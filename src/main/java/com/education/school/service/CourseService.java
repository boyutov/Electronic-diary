package com.education.school.service;

import com.education.school.dto.CourseRequest;
import com.education.school.entity.Course;
import com.education.school.entity.Teacher;
import com.education.school.repository.CourseRepository;
import com.education.school.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public Course findById(Integer id) {
        return courseRepository.findById(id).orElse(null);
    }

    @Transactional
    public Course create(CourseRequest request) {
        Teacher teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        Course course = new Course();
        course.setName(request.name());
        course.setTeacher(teacher);
        course.setDescription(request.description());

        return courseRepository.save(course);
    }

    @Transactional
    public Course update(Integer id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        Teacher teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        course.setName(request.name());
        course.setTeacher(teacher);
        course.setDescription(request.description());

        return courseRepository.save(course);
    }

    @Transactional
    public void delete(Integer id) {
        courseRepository.deleteById(id);
    }
}
