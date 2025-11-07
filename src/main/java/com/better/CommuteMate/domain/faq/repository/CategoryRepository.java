package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 대분류 이름으로 조회
    Category findByName(String name);

    // 존재 여부 확인
    boolean existsByName(String name);
}