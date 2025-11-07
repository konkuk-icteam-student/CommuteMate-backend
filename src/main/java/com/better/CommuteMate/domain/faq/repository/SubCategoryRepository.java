package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.category.entity.SubCategory;
import com.better.CommuteMate.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    // 특정 대분류(Category)에 속한 모든 소분류 조회
    List<SubCategory> findByCategory(Category category);

    // 소분류 이름으로 조회
    SubCategory findByName(String name);

    // 특정 대분류 ID로 소분류 조회 (Category 객체가 없는 경우)
    List<SubCategory> findByCategoryId(Long categoryId);

    // 중복 확인용
    boolean existsByNameAndCategory(String name, Category category);
}