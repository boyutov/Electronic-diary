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

    @Transactional(readOnly = true)
    public GroupEntity findById(Integer id) {
        return groupRepository.findById(id).orElse(null);
    }

    @Transactional
    public GroupEntity create(GroupRequest request) {
        GroupEntity group = new GroupEntity();
        group.setName(request.name());
        group.setHasOffice(request.hasOffice());
        group.setOffice(request.office());
        group.setCourse(request.course());
        group.setFundingType(request.fundingType() != null ? request.fundingType() : "BUDGET");

        return groupRepository.save(group);
    }

    @Transactional
    public GroupEntity update(Integer id, GroupRequest request) {
        GroupEntity group = groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        group.setName(request.name());
        group.setHasOffice(request.hasOffice());
        group.setOffice(request.office());
        group.setCourse(request.course());
        group.setFundingType(request.fundingType() != null ? request.fundingType() : "BUDGET");

        return groupRepository.save(group);
    }

    @Transactional
    public void delete(Integer id) {
        groupRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByGroupId(Integer groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Контроллер уже защищен @PreAuthorize("hasAnyAuthority('TEACHER', 'ADMIN')"),
        // поэтому любой авторизованный учитель имеет право получить список учеников для выставления оценок.
        return group.getStudents().stream()
                .map(StudentDto::from)
                .collect(Collectors.toList());
    }
}
