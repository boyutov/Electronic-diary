package com.education.school.service;

import com.education.school.dto.PollRequest;
import com.education.school.entity.Poll;
import com.education.school.entity.PollOption;
import com.education.school.entity.User;
import com.education.school.repository.PollRepository;
import com.education.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final UserRepository userRepository;

    public List<Poll> findAll() {
        return pollRepository.findAll();
    }

    @Transactional
    public Poll create(PollRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User createdByUser = userRepository.findByEmail(email);

        Poll poll = new Poll();
        poll.setTitle(request.title());
        poll.setDescription(request.description());
        poll.setActive(request.active());
        poll.setCreatedAt(LocalDateTime.now());
        poll.setCreatedByUser(createdByUser);

        List<PollOption> options = request.options().stream().map(optionText -> {
            PollOption pollOption = new PollOption();
            pollOption.setOptionText(optionText);
            pollOption.setPoll(poll);
            return pollOption;
        }).collect(Collectors.toList());

        poll.getOptions().addAll(options);

        return pollRepository.save(poll);
    }
}
