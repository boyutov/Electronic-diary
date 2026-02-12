package com.education.school.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.WebApplicationContext;

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

    @GetMapping("/{schoolName}/admin")
    public String admin(@PathVariable String schoolName) {
        return "admin";
    }

    @GetMapping("/{schoolName}/admin/admin")
    public String adminAdmin(@PathVariable String schoolName) {
        return "admin-admin";
    }

    @GetMapping("/{schoolName}/admin/director")
    public String adminDirector(@PathVariable String schoolName) {
        return "admin-director";
    }

    @GetMapping("/{schoolName}/admin/teacher")
    public String adminTeacher(@PathVariable String schoolName) {
        return "admin-teacher";
    }

    @GetMapping("/{schoolName}/admin/student")
    public String adminStudent(@PathVariable String schoolName) {
        return "admin-student";
    }

    @GetMapping("/{schoolName}/curator/student")
    public String curatorStudent(@PathVariable String schoolName) {
        return "curator-student";
    }

    @GetMapping("/{schoolName}/admin/parent")
    public String adminParent(@PathVariable String schoolName) {
        return "admin-parent";
    }

    @GetMapping("/{schoolName}/admin/group")
    public String adminGroup(@PathVariable String schoolName) {
        return "admin-group";
    }

    @GetMapping("/{schoolName}/admin/discipline")
    public String adminDiscipline(@PathVariable String schoolName) {
        return "admin-discipline";
    }

    @GetMapping("/{schoolName}/admin/course")
    public String adminCourse(@PathVariable String schoolName) {
        return "admin-course";
    }

    @GetMapping("/{schoolName}/admin/schedule")
    public String adminSchedule(@PathVariable String schoolName) {
        return "admin-schedule";
    }

    @GetMapping("/{schoolName}/admin/news")
    public String adminNews(@PathVariable String schoolName) {
        return "admin-news";
    }

    @GetMapping("/{schoolName}/admin/poll")
    public String adminPoll(@PathVariable String schoolName) {
        return "admin-poll";
    }

    @GetMapping("/{schoolName}/director")
    public String director(@PathVariable String schoolName) {
        return "director";
    }

    @GetMapping("/{schoolName}/teacher")
    public String teacher(@PathVariable String schoolName) {
        return "teacher";
    }

    @GetMapping("/{schoolName}/student")
    public String student(@PathVariable String schoolName) {
        return "student";
    }

    @GetMapping("/{schoolName}/parent")
    public String parent(@PathVariable String schoolName) {
        return "parent";
    }

    @GetMapping("/{schoolName}/profile")
    public String profile(@PathVariable String schoolName) {
        return "profile";
    }
}
