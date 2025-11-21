package com.better.CommuteMate.domain.category.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sub_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;  // 소분류명

    // FK: category_id → category(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 즐겨찾기 여부 (기본값 false)
    @Column(name = "favorite", nullable = false)
    @Builder.Default
    private boolean favorite = false;

    // 이 subCategory를 어떤 users가 담당하는지 저장하는 리스트
    @OneToMany(mappedBy = "subCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ManagerSubCategory> managers = new ArrayList<>();

    public void updateName(String name) {
        this.name = name;
    }

    public void updateFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}