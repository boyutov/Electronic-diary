package com.education.school.service;

import com.education.school.dto.CourseRequest;
import com.education.school.dto.CourseResponse;
import com.education.school.entity.*;
import com.education.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Course findById(Integer id) {
        return courseRepository.findById(id).orElse(null);
    }

    /** Все курсы с флагом enrolled для текущего студента */
    @Transactional(readOnly = true)
    public List<CourseResponse> findAllForStudent() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        Student student = studentRepository.findByUserId(user.getId()).orElse(null);

        return courseRepository.findAll().stream()
                .map(c -> {
                    boolean enrolled = student != null &&
                            c.getStudents().stream().anyMatch(s -> s.getId().equals(student.getId()));
                    return CourseResponse.from(c, enrolled, false);
                })
                .collect(Collectors.toList());
    }

    /** Курсы текущего учителя со списком студентов */
    @Transactional(readOnly = true)
    public List<CourseResponse> findMyCoursesAsTeacher() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Teacher not found"));

        return courseRepository.findAll().stream()
                .filter(c -> c.getTeacher() != null && c.getTeacher().getId().equals(teacher.getId()))
                .map(c -> CourseResponse.from(c, false, true))
                .collect(Collectors.toList());
    }

    /** Записать текущего студента на курс */
    @Transactional
    public CourseResponse enroll(Integer courseId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Student profile not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (course.getStudents().stream().anyMatch(s -> s.getId().equals(student.getId()))) {
            throw new IllegalStateException("Already enrolled");
        }

        course.getStudents().add(student);
        return CourseResponse.from(courseRepository.save(course), true, false);
    }

    /** Отписать текущего студента от курса */
    @Transactional
    public CourseResponse unenroll(Integer courseId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Student profile not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        course.getStudents().removeIf(s -> s.getId().equals(student.getId()));
        return CourseResponse.from(courseRepository.save(course), false, false);
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
