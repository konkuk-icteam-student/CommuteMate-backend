package com.better.CommuteMate.domain.category.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;  // 분류명

    // 즐겨찾기 여부 (기본값 false)
    @Column(name = "favorite", nullable = false)
    private boolean favorite = false;

    // 카테고리 담당자 리스트
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ManagerCategory> managers = new ArrayList<>();

    public Category(String name) {
        this.name = name;
    }

    public void updateName(String newName) {
        this.name = newName;
    }

    public void updateFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
