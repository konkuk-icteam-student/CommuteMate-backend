package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
