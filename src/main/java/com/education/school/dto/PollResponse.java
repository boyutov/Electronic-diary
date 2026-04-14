package com.education.school.dto;

import com.education.school.entity.Poll;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class PollResponse {
    private Integer id;
    private String title;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private String allowedRoles;
    private List<OptionDto> options;
    private boolean votedByMe;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class OptionDto {
        private Integer id;
        private String optionText;
        private int voteCount;
    }

    public static PollResponse from(Poll poll) {
        return from(poll, false);
    }

    public static PollResponse from(Poll poll, boolean votedByMe) {
        List<OptionDto> options = poll.getOptions().stream()
                .map(o -> new OptionDto(o.getId(), o.getOptionText(), o.getVotes().size()))
                .sorted((a, b) -> Integer.compare(b.getVoteCount(), a.getVoteCount()))
                .collect(Collectors.toList());
        return new PollResponse(
            poll.getId(),
            poll.getTitle(),
            poll.getDescription(),
            poll.getActive(),
            poll.getCreatedAt(),
            poll.getAllowedRoles(),
            options,
            votedByMe
        );
    }
}
