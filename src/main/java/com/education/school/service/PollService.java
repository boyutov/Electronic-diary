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
        Poll poll = pollRepository.findById(id).orElse(null);
        if (poll == null) return null;
        User user = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        String role = user.getRole().getName();
        if (!List.of("ADMIN", "DIRECTOR").contains(role)
                && (!Boolean.TRUE.equals(poll.getActive()) || !isRoleAllowed(poll, role))) {
            throw new SecurityException("Этот опрос недоступен");
        }
        return poll;
    }

    /** Голосования доступные текущему пользователю (по его роли) */
    @Transactional(readOnly = true)
    public List<PollResponse> findAllForCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        String role = user.getRole().getName();

        return pollRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> isRoleAllowed(p, role))
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

        // Ответ API сортирует варианты по голосам, поэтому порядок текста не означает замену.
        boolean optionsChanged = !poll.getOptions().stream().map(PollOption::getOptionText).sorted().toList()
                .equals(request.options().stream().sorted().toList());
        if (optionsChanged && (!poll.getVotes().isEmpty()
                || poll.getOptions().stream().anyMatch(o -> !o.getVotes().isEmpty()))) {
            throw new IllegalArgumentException("Нельзя менять варианты опроса после начала голосования");
        }

        poll.setTitle(request.title());
        poll.setDescription(request.description());
        poll.setActive(request.active());
        if (request.allowedRoles() != null) {
            poll.setAllowedRoles(request.allowedRoles());
        }

        if (optionsChanged) {
            poll.getOptions().clear();
            for (String optionText : request.options()) {
                PollOption option = new PollOption();
                option.setOptionText(optionText);
                option.setPoll(poll);
                poll.getOptions().add(option);
            }
        }

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
        // Голосовать могут только роли, указанные в опросе (например, опрос только для учителей)
        if (!isRoleAllowed(poll, user.getRole().getName())) {
            throw new SecurityException("Этот опрос не для вашей роли");
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

    private boolean isRoleAllowed(Poll poll, String role) {
        String allowed = poll.getAllowedRoles();
        return allowed == null || allowed.isBlank()
                || java.util.Arrays.stream(allowed.split(","))
                .anyMatch(r -> r.trim().equalsIgnoreCase(role));
    }

    @Transactional
    public void delete(Integer id) {
        pollRepository.deleteById(id);
    }
}
