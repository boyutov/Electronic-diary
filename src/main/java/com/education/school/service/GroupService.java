package com.education.school.service;

import com.education.school.dto.GroupRequest;
import com.education.school.dto.GroupResponse;
import com.education.school.dto.StudentDto;
import com.education.school.entity.GroupEntity;
import com.education.school.entity.User;
import com.education.school.repository.GroupRepository;
import com.education.school.repository.TeacherRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GroupResponse> findAll() {
        return groupRepository.findAll().stream()
                .map(GroupResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupEntity create(GroupRequest request) {
        GroupEntity group = new GroupEntity();
        group.setName(request.name());
        group.setHasOffice(request.hasOffice());
        group.setOffice(request.office());
        group.setCourse(request.course());

        return groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByGroupId(Integer groupId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Security check: only the curator of this group or an admin can view students
        if (group.getCurator() != null && !group.getCurator().getId().equals(currentUser.getId()) && !currentUser.getRole().getName().equals("ADMIN")) {
            throw new SecurityException("You are not authorized to view students of this group.");
        }

        return group.getStudents().stream()
                .map(StudentDto::from)
                .collect(Collectors.toList());
    }
}
