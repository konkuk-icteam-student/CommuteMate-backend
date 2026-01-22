package com.better.CommuteMate.domain.category.repository;

import com.better.CommuteMate.domain.category.entity.ManagerCategory;
import com.better.CommuteMate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerCategoryRepository extends JpaRepository<ManagerCategory, Long> {
    boolean existsByManagerIdAndCategoryIdAndActiveTrue(Long managerId, Long categoryId);
}