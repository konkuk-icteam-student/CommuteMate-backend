package com.better.CommuteMate.domain.category.entity;

import com.better.CommuteMate.domain.manager.entity.Manager;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "manager_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagerCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "favorite", nullable = false)
    private boolean favorite = false;

     //insert 직전에 자동으로 assignedAt 세팅
    @PrePersist
    protected void onCreate() {
        this.assignedAt = LocalDateTime.now();
    }

    public static ManagerCategory assign(Manager manager, Category category) {
        ManagerCategory mc = new ManagerCategory();
        mc.manager = manager;
        mc.category = category;
        return mc;
    }

    public void updateFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
