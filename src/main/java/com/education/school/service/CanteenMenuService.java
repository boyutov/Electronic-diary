package com.education.school.service;

import com.education.school.dto.CanteenMenuDto;
import com.education.school.entity.*;
import com.education.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CanteenMenuService {

    private final CanteenMenuRepository menuRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    @Transactional(readOnly = true)
    public List<CanteenMenuDto> findAll() {
        return menuRepository.findAll().stream()
                .map(CanteenMenuDto::from)
                .collect(Collectors.toList());
    }

    /** Меню доступные текущему пользователю по его группе */
    @Transactional(readOnly = true)
    public List<CanteenMenuDto> findForCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        String role = user.getRole().getName();

        List<CanteenMenu> all = menuRepository.findAll();

        // ADMIN, DIRECTOR, TEACHER, MINISTRY — видят все
        if (List.of("ADMIN", "DIRECTOR", "TEACHER", "MINISTRY").contains(role)) {
            return all.stream().map(CanteenMenuDto::from).collect(Collectors.toList());
        }

        // STUDENT — только меню своей группы
        if ("STUDENT".equals(role)) {
            return studentRepository.findByUserId(user.getId())
                    .map(student -> {
                        if (student.getGroup() == null) return List.<CanteenMenuDto>of();
                        Integer groupId = student.getGroup().getId();
                        return all.stream()
                                .filter(m -> m.getGroups().isEmpty() ||
                                        m.getGroups().stream().anyMatch(g -> g.getId().equals(groupId)))
                                .map(CanteenMenuDto::from)
                                .collect(Collectors.toList());
                    }).orElse(List.of());
        }

        // PARENT — меню групп своих детей
        if ("PARENT".equals(role)) {
            return parentRepository.findByUserEmail(email)
                    .map(parent -> {
                        List<Integer> childGroupIds = parent.getStudents().stream()
                                .filter(s -> s.getGroup() != null)
                                .map(s -> s.getGroup().getId())
                                .collect(Collectors.toList());
                        if (childGroupIds.isEmpty()) return List.<CanteenMenuDto>of();
                        return all.stream()
                                .filter(m -> m.getGroups().isEmpty() ||
                                        m.getGroups().stream().anyMatch(g -> childGroupIds.contains(g.getId())))
                                .map(CanteenMenuDto::from)
                                .collect(Collectors.toList());
                    }).orElse(List.of());
        }

        return List.of();
    }

    @Transactional
    public CanteenMenuDto create(Map<String, Object> request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);

        CanteenMenu menu = new CanteenMenu();
        menu.setTitle((String) request.get("title"));
        menu.setDescription((String) request.get("description"));
        menu.setImageData((String) request.get("imageData"));
        menu.setImageType((String) request.get("imageType"));
        menu.setMenuDate(request.get("menuDate") != null
                ? LocalDate.parse((String) request.get("menuDate"))
                : LocalDate.now());
        menu.setCreatedAt(LocalDateTime.now());
        menu.setCreatedByUser(user);

        @SuppressWarnings("unchecked")
        List<Integer> groupIds = (List<Integer>) request.get("groupIds");
        if (groupIds != null && !groupIds.isEmpty()) {
            menu.getGroups().addAll(groupRepository.findAllById(groupIds));
        }

        return CanteenMenuDto.from(menuRepository.save(menu));
    }

    @Transactional
    public CanteenMenuDto update(Integer id, Map<String, Object> request) {
        CanteenMenu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found"));

        menu.setTitle((String) request.get("title"));
        menu.setDescription((String) request.get("description"));
        if (request.get("imageData") != null) {
            menu.setImageData((String) request.get("imageData"));
            menu.setImageType((String) request.get("imageType"));
        }
        if (request.get("menuDate") != null) {
            menu.setMenuDate(LocalDate.parse((String) request.get("menuDate")));
        }

        @SuppressWarnings("unchecked")
        List<Integer> groupIds = (List<Integer>) request.get("groupIds");
        if (groupIds != null) {
            menu.getGroups().clear();
            menu.getGroups().addAll(groupRepository.findAllById(groupIds));
        }

        return CanteenMenuDto.from(menuRepository.save(menu));
    }

    @Transactional
    public void delete(Integer id) {
        menuRepository.deleteById(id);
    }
}
