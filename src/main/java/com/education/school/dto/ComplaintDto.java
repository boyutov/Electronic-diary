package com.education.school.dto;

import com.education.school.entity.Complaint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintDto {
    private Integer id;
    private String content;
    private OffsetDateTime createdAt;
    private Boolean isAnonymous;
    private Long authorUserId;

    public static ComplaintDto from(Complaint complaint) {
        return new ComplaintDto(
                complaint.getId(),
                complaint.getContent(),
                complaint.getCreatedAt(),
                complaint.getIsAnonymous(),
                complaint.getAuthorUser() != null ? complaint.getAuthorUser().getId() : null
        );
    }
}
