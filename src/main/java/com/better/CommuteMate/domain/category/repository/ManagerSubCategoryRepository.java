package com.better.CommuteMate.domain.category.repository;

import com.better.CommuteMate.domain.category.entity.ManagerSubCategory;
import com.better.CommuteMate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerSubCategoryRepository extends JpaRepository<ManagerSubCategory, Long> {
    boolean existsByManager_UserIdAndSubCategory_Id(Integer userId, Integer subCategoryId);
    void deleteAllByManager(User manager);
}