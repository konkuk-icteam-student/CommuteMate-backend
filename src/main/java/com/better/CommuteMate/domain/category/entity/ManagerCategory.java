package com.better.CommuteMate.domain.category.entity;

import com.better.CommuteMate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "manager_category")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ManagerCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User manager; // User 중 manager role을 가진 사람만 가능 (검증 로직 있어야 함)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @JoinColumn(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

     //insert 직전에 자동으로 assignedAt 세팅
    @PrePersist
    protected void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = LocalDateTime.now();
        }
    }

    public static ManagerCategory of(User manager, Category category) {
        return ManagerCategory.builder()
                .manager(manager)
                .category(category)
                .build();
    }
}
