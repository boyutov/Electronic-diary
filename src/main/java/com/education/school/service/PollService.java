package com.education.school.service;

import com.education.school.dto.PollRequest;
import com.education.school.dto.PollResponse;
import com.education.school.entity.Poll;
import com.education.school.entity.PollOption;
import com.education.school.entity.PollVote;
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

    @Transactional(readOnly = true)
    public List<Poll> findAll() {
        return pollRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Poll findById(Integer id) {
        return pollRepository.findById(id).orElse(null);
    }

    /** Голосования доступные текущему пользователю (по его роли) */
    @Transactional(readOnly = true)
    public List<PollResponse> findAllForCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        String role = user.getRole().getName();

        return pollRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> {
                    String allowed = p.getAllowedRoles();
                    if (allowed == null || allowed.isBlank()) return true;
                    for (String r : allowed.split(",")) {
                        if (r.trim().equalsIgnoreCase(role)) return true;
                    }
                    return false;
                })
                .map(p -> {
                    boolean voted = p.getVotes().stream()
                            .anyMatch(v -> v.getUser().getId().equals(user.getId()));
                    return PollResponse.from(p, voted);
                })
                .collect(Collectors.toList());
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
        poll.setAllowedRoles(request.allowedRoles() != null ? request.allowedRoles() : "STUDENT,TEACHER,PARENT,DIRECTOR");

        List<PollOption> options = request.options().stream().map(optionText -> {
            PollOption pollOption = new PollOption();
            pollOption.setOptionText(optionText);
            pollOption.setPoll(poll);
            return pollOption;
        }).collect(Collectors.toList());

        poll.getOptions().addAll(options);
        return pollRepository.save(poll);
    }

    @Transactional
    public Poll update(Integer id, PollRequest request) {
        Poll poll = pollRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        poll.setTitle(request.title());
        poll.setDescription(request.description());
        poll.setActive(request.active());
        if (request.allowedRoles() != null) {
            poll.setAllowedRoles(request.allowedRoles());
        }

        poll.getOptions().clear();
        List<PollOption> options = request.options().stream().map(optionText -> {
            PollOption pollOption = new PollOption();
            pollOption.setOptionText(optionText);
            pollOption.setPoll(poll);
            return pollOption;
        }).collect(Collectors.toList());
        poll.getOptions().addAll(options);

        return pollRepository.save(poll);
    }

    @Transactional
    public PollResponse vote(Integer pollId, Integer optionId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        if (!Boolean.TRUE.equals(poll.getActive())) {
            throw new IllegalStateException("Poll is not active");
        }

        boolean alreadyVoted = poll.getVotes().stream()
                .anyMatch(v -> v.getUser().getId().equals(user.getId()));
        if (alreadyVoted) {
            throw new IllegalStateException("Already voted");
        }

        PollOption option = poll.getOptions().stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Option not found"));

        PollVote vote = new PollVote();
        vote.setPoll(poll);
        vote.setOption(option);
        vote.setUser(user);
        vote.setVotedAt(LocalDateTime.now());
        option.getVotes().add(vote);

        Poll saved = pollRepository.save(poll);
        return PollResponse.from(saved, true);
    }

    @Transactional
    public void delete(Integer id) {
        pollRepository.deleteById(id);
    }
}
