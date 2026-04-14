package com.education.school.dto;

import com.education.school.entity.CanteenMenu;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class CanteenMenuDto {
    private Integer id;
    private String title;
    private String description;
    private String imageData;
    private String imageType;
    private LocalDate menuDate;
    private List<Integer> groupIds;
    private List<String> groupNames;

    public static CanteenMenuDto from(CanteenMenu menu) {
        return new CanteenMenuDto(
            menu.getId(),
            menu.getTitle(),
            menu.getDescription(),
            menu.getImageData(),
            menu.getImageType(),
            menu.getMenuDate(),
            menu.getGroups().stream().map(g -> g.getId()).collect(Collectors.toList()),
            menu.getGroups().stream().map(g -> g.getName()).collect(Collectors.toList())
        );
    }
}
