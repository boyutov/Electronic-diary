package com.education.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Меню столовой на конкретный день
@Setter
@Getter
@Entity
@Table(name = "canteen_menus")
public class CanteenMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;  // состав меню

    // Изображение блюда — хранится как Base64 строка прямо в БД (не рекомендуется для больших файлов)
    @Column(name = "image_data", columnDefinition = "TEXT")
    private String imageData;

    @Column(name = "image_type")
    private String imageType;  // MIME-тип: "image/jpeg", "image/png"

    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate;  // на какой день это меню

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    // Группы для которых предназначено это меню — Many-to-Many через canteen_menu_groups
    @ManyToMany
    @JoinTable(
        name = "canteen_menu_groups",
        joinColumns = @JoinColumn(name = "menu_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<GroupEntity> groups = new HashSet<>();
}
