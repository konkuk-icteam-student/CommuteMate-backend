package com.better.CommuteMate.domain.category.entity;

import com.better.CommuteMate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "manager_subcategory")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ManagerSubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User manager; // User 중 manager role을 가진 사람만 가능 (검증 로직 있어야 함)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", nullable = false)
    private SubCategory subCategory;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    @Column(nullable = false)
    private boolean active = true;

    public static ManagerSubCategory of(User manager, SubCategory subCategory) {
        return ManagerSubCategory.builder()
                .manager(manager)
                .subCategory(subCategory)
                .build();
    }
}
