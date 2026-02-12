package com.education.school.service;

import com.education.school.dto.ComplaintDto;
import com.education.school.dto.ComplaintRequest;
import com.education.school.entity.Complaint;
import com.education.school.entity.User;
import com.education.school.repository.ComplaintRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ComplaintDto> findAll() {
        return complaintRepository.findAll().stream()
                .filter(c -> c.getDeletedAt() == null)
                .map(ComplaintDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ComplaintDto create(ComplaintRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User author = userRepository.findByEmail(email);

        Complaint complaint = new Complaint();
        complaint.setContent(request.content());
        complaint.setIsAnonymous(request.isAnonymous() != null ? request.isAnonymous() : false);
        complaint.setCreatedAt(OffsetDateTime.now());
        complaint.setAuthorUser(author);

        complaint = complaintRepository.save(complaint);
        return ComplaintDto.from(complaint);
    }

    @Transactional
    public void delete(Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email);

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

        if (!complaint.getAuthorUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only author can delete complaint");
        }

        complaint.setDeletedAt(OffsetDateTime.now());
        complaintRepository.save(complaint);
    }
}
