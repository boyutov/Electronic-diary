package com.education.school.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/pricing")
    public String pricing() {
        return "pricing";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/activate")
    public String activate() {
        return "activate";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/admin/admin")
    public String adminAdmin() {
        return "admin-admin";
    }

    @GetMapping("/admin/director")
    public String adminDirector() {
        return "admin-director";
    }

    @GetMapping("/admin/teacher")
    public String adminTeacher() {
        return "admin-teacher";
    }

    @GetMapping("/admin/student")
    public String adminStudent() {
        return "admin-student";
    }

    @GetMapping("/admin/parent")
    public String adminParent() {
        return "admin-parent";
    }

    @GetMapping("/admin/group")
    public String adminGroup() {
        return "admin-group";
    }

    @GetMapping("/admin/discipline")
    public String adminDiscipline() {
        return "admin-discipline";
    }

    @GetMapping("/admin/course")
    public String adminCourse() {
        return "admin-course";
    }

    @GetMapping("/admin/schedule")
    public String adminSchedule() {
        return "admin-schedule";
    }

    @GetMapping("/admin/news")
    public String adminNews() {
        return "admin-news";
    }

    @GetMapping("/admin/poll")
    public String adminPoll() {
        return "admin-poll";
    }

    @GetMapping("/director")
    public String director() {
        return "director";
    }

    @GetMapping("/teacher")
    public String teacher() {
        return "teacher";
    }

    @GetMapping("/student")
    public String student() {
        return "student";
    }

    @GetMapping("/parent")
    public String parent() {
        return "parent";
    }
}
