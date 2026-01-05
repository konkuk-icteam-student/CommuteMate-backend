package com.better.CommuteMate.domain.category.repository;

import com.better.CommuteMate.domain.category.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    boolean existsByCategoryIdAndName(Long categoryId, String name);

    // SubCategory 이름으로 단일 조회
    Optional<SubCategory> findByName(String name);

    // 중복 등록 방지를 위해 존재 여부 확인용
    boolean existsByName(String name);
}