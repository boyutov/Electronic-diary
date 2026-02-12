package com.education.school.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class SchoolWebAuthenticationDetails extends WebAuthenticationDetails {

    private final String schoolName;

    public SchoolWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.schoolName = request.getParameter("schoolName");
    }

    public String getSchoolName() {
        return schoolName;
    }
}
