package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
}
